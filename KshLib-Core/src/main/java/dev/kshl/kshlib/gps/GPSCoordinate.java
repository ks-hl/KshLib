package dev.kshl.kshlib.gps;

import dev.kshl.kshlib.misc.BitBuffer;
import dev.kshl.kshlib.misc.Bits;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPSCoordinate {
    private final String coordinate;
    private final int degree;
    private final int minute;
    private final double second;

    public Direction getDirection() {
        return direction;
    }

    private final Direction direction;
    private final double decimal;

    public CoordinateType getCoordinateType() {
        return coordinateType;
    }

    private final CoordinateType coordinateType;

    private static final double EARTH_CIRCUMFERENCE_MILES = 24901;
    private static final double MILES_PER_DEGREE = EARTH_CIRCUMFERENCE_MILES / 360;
    private static final double FEET_PER_MILE = 5280.0;
    static final double BYTE_STORAGE_POINT = EARTH_CIRCUMFERENCE_MILES * FEET_PER_MILE / 360; // Provides 1 foot accuracy. 24901 * 5280 / 360
    static final double STORAGE_ERROR = 1 / GPSCoordinate.BYTE_STORAGE_POINT / 2;

    public GPSCoordinate(String coordinate) {
        this.coordinate = coordinate;
        DegreeMinuteSecond dms = parseCoordinate(coordinate);
        this.degree = dms.degree();
        this.minute = dms.minute();
        this.second = dms.seconds();
        this.direction = dms.direction();
        this.decimal = calculateDecimal(degree, minute, second, direction);
        this.coordinateType = direction.getCoordinateType();
    }

    public GPSCoordinate(int degree, int minute, double second, Direction direction) {
        this.degree = degree;
        this.minute = minute;
        this.second = second;
        this.direction = direction;
        this.decimal = calculateDecimal(degree, minute, second, direction);
        this.coordinateType = direction.getCoordinateType();
        this.coordinate = toString();
    }

    public GPSCoordinate(double coordinate, CoordinateType coordinateType) {
        this.coordinateType = coordinateType;
        double absCoordinate = Math.abs(coordinate);
        this.degree = (int) absCoordinate;
        double fractional = absCoordinate - degree;
        this.minute = (int) (fractional * 60);
        this.second = (fractional * 60 - minute) * 60;
        this.decimal = coordinate;

        this.direction = determineDirection();
        this.coordinate = toString();
    }

    public GPSCoordinate(byte[] coordinate, CoordinateType coordinateType) {
        this(ByteBuffer.wrap(coordinate).getInt(), coordinateType);
    }

    public GPSCoordinate(int integerEquivalentCoordinate, CoordinateType coordinateType) {
        this.coordinateType = coordinateType;

        this.decimal = integerEquivalentCoordinate / BYTE_STORAGE_POINT;

        double absCoordinate = Math.abs(decimal);
        this.degree = (int) absCoordinate;
        double fractional = absCoordinate - degree;
        this.minute = (int) (fractional * 60);
        this.second = (fractional * 60 - minute) * 60;

        this.direction = determineDirection();
        this.coordinate = toString();
    }

    public int toInteger() {
        return (int) Math.round(decimal * BYTE_STORAGE_POINT);
    }

    public byte[] toByteArray() {
        return ByteBuffer.allocate(4).putInt(toInteger()).array();
    }

    public double toDecimal() {
        return decimal;
    }

    public DegreeMinuteSecond getDegreeMinuteSecond() {
        return new DegreeMinuteSecond(degree, minute, second, direction);
    }

    @Override
    public String toString() {
        return coordinate != null ? coordinate : String.format("%d°%d'%s\"%s", degree, minute, second, direction.name());
    }

    private DegreeMinuteSecond parseCoordinate(String coordinate) {
        Pattern pattern = Pattern.compile("(\\d+)°\\s*(\\d+)'\\s*(\\d+(?:\\.\\d+)?)\"\\s*([NSEW])");
        Matcher matcher = pattern.matcher(coordinate);
        if (matcher.matches()) {
            int degree = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            double second = Double.parseDouble(matcher.group(3));
            Direction direction = Direction.valueOf(matcher.group(4));
            return new DegreeMinuteSecond(degree, minute, second, direction);
        } else {
            throw new IllegalArgumentException("Invalid coordinate format");
        }
    }

    private Direction determineDirection() {
        if (decimal < 0) {
            return coordinateType == CoordinateType.LATITUDE ? Direction.S : Direction.W;
        } else {
            return coordinateType == CoordinateType.LATITUDE ? Direction.N : Direction.E;
        }
    }

    private double calculateDecimal(int degree, int minute, double second, Direction direction) {
        double decimal = degree + (minute / 60.0) + (second / 3600.0);
        return (direction == Direction.S || direction == Direction.W) ? -decimal : decimal;
    }

    public int getBitsRequiredForStorage() {
        return (int) Math.ceil(Math.log(Math.abs(toInteger()) + 1) / Math.log(2)) + 1;
    }


    public static GPSCoordinatePair fromByteArray(byte[] data) {
        BitBuffer buffer = new BitBuffer(data);
        int bitsEach = data.length * 8 / 2;

        int pad = (32 - bitsEach) / 8;
        int lat = Bits.toInt(Bits.pad(buffer.get(0, bitsEach), pad)) >> (32 - bitsEach);
        int lon = Bits.toInt(Bits.pad(buffer.get(bitsEach, buffer.size()), pad)) >> (32 - bitsEach);

        return new GPSCoordinatePair(new GPSCoordinate(lat, CoordinateType.LATITUDE), new GPSCoordinate(lon, CoordinateType.LONGITUDE));
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GPSCoordinate otherCoordinate)) return false;

        if (!coordinateType.equals(otherCoordinate.coordinateType)) return false;
        if (!direction.equals(otherCoordinate.direction)) return false;

        return Math.abs(decimal - otherCoordinate.decimal) < 0.5 / BYTE_STORAGE_POINT;
    }

    public GPSCoordinate round() {
        return new GPSCoordinate(toInteger(), coordinateType);
    }

    public static boolean canBeEncoded(GPSPositionList list) {
        try {
            encodeCoordinateSequence(list);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static byte[] encodeCoordinateSequence(GPSPositionList pairs) {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);

        long lastEncodedTime = 0;
        GPSCoordinatePair encodedPosition = null;
        for (int i = 0; i < pairs.size(); i++) {
            GPSTimeAndCoordinate pair = pairs.get(i);

            if (i == 0) {
                lastEncodedTime = pair.time();
                encodedPosition = pair.coordinates();
                buffer.putLong(pair.time());
                buffer.put(pair.coordinates().toByteArray(7));
            } else {
                GPSCoordinatePair delta = encodedPosition.deltaTo(pair.coordinates());
                encodedPosition = encodedPosition.add(delta.round());
                int bytesRequired = delta.getBytesRequired();
                if (bytesRequired > 4) {
                    throw new IllegalArgumentException("Distance delta too great");
                }

                BitBuffer bitBuffer = new BitBuffer(1);

                long encodedTime = Math.round((pair.time() - lastEncodedTime) / 1000D);
                if (encodedTime < 0) {
                    throw new IllegalArgumentException("Entry occurred before preceding entry");
                }
                if (encodedTime > 63) {
                    throw new IllegalArgumentException("Time delta too long");
                }
                lastEncodedTime += encodedTime * 1000;

                bitBuffer.put(0, new byte[]{(byte) encodedTime}, 2, 8);
                bitBuffer.put(6, new byte[]{(byte) (bytesRequired - 1)}, 6, 8);

                buffer.put(bitBuffer.array());

                buffer.put(delta.toByteArray(bytesRequired));
            }
        }
        return Bits.slice(buffer, 0, buffer.position()).array();
    }

    public static GPSPositionList decodeCoordinateSequence(byte[] bytes) {
        return decodeCoordinateSequence(ByteBuffer.wrap(bytes), -1);
    }

    public static GPSPositionList decodeCoordinateSequence(ByteBuffer buffer, int limit) {
        StringBuilder debugData = new StringBuilder("Debug Data");
        try {
            if (limit <= 0) limit = Integer.MAX_VALUE;
            GPSPositionList out = new GPSPositionList();

            long start = buffer.getLong();
            debugData.append("\nStart Time: ").append(start);
            byte[] firstBytes = new byte[7];
            buffer.get(firstBytes);
            GPSCoordinatePair currentCoordinates = fromByteArray(firstBytes);
            debugData.append("\nStart Position: ").append(currentCoordinates);
            out.add(new GPSTimeAndCoordinate(start, currentCoordinates));

            long lastTime = start;
            while (buffer.position() < buffer.array().length && out.size() < limit) {
                debugData.append("\n");
                debugData.append(buffer.position()).append(". ");
                byte header = buffer.get();

                debugData.append(Bits.toBinaryString(new byte[]{header})).append("=");

                long timeDelta = Math.round(((header >>> 2)) * 1000D);

                byte[] coordinateData = new byte[(header & 0b00000011) + 1];

                debugData.append(timeDelta).append("ms/").append(coordinateData.length).append("B: ");

                buffer.get(coordinateData);

                debugData.append(Bits.encodeToHex(coordinateData));

                GPSTimeAndCoordinate gpsTimeAndCoordinate = new GPSTimeAndCoordinate(lastTime += timeDelta, currentCoordinates = currentCoordinates.add(fromByteArray(coordinateData)));
                debugData.append(" = ").append(gpsTimeAndCoordinate);
                out.add(gpsTimeAndCoordinate);
            }

            return out;
        } catch (Throwable t) {
            System.err.println(debugData);
            throw t;
        }
    }

    static double decimalToFeet(double decimal) {
        return decimal * MILES_PER_DEGREE * FEET_PER_MILE;
    }

    static double feetToDecimal(double feet) {
        return feet / (MILES_PER_DEGREE * FEET_PER_MILE);
    }
}
