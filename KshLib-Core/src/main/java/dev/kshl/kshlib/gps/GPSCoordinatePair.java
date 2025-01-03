package dev.kshl.kshlib.gps;

import dev.kshl.kshlib.misc.BitBuffer;
import dev.kshl.kshlib.misc.Bits;

public record GPSCoordinatePair(GPSCoordinate latitude, GPSCoordinate longitude) {
    public GPSCoordinatePair {
        if (latitude.getCoordinateType() != CoordinateType.LATITUDE) {
            throw new IllegalArgumentException("Latitude coordinate is not type LATITUDE");
        }
        if (longitude.getCoordinateType() != CoordinateType.LONGITUDE) {
            throw new IllegalArgumentException("Longitude coordinate is not type LONGITUDE");
        }
    }

    @Override
    public String toString() {
        return latitude() + ", " + longitude();
    }

    public byte[] toByteArray(int bytes) {
        if (bytes > 7) throw new IllegalArgumentException("No more than 7 bytes needed");
        if (bytes < 1) throw new IllegalArgumentException("Must use at least 1 byte");

        BitBuffer bitBuffer = new BitBuffer(bytes);
        int bitsEach = bytes * 8 / 2;

        int lat = latitude().toInteger();
        int lon = longitude().toInteger();

        int max = (int) Math.pow(2, bitsEach - 1) - 1;
        if (lat > max || lat < -max - 1) {
            throw new IllegalArgumentException("|Latitude| too high to use " + bytes + " bytes. val=" + lat + ",max=" + max + ",coordinate=" + latitude());
        }
        if (lon > max || lon < -max - 1) {
            throw new IllegalArgumentException("|Longitude| too high to use " + bytes + " bytes. val=" + lon + ",max=" + max + ",coordinate=" + longitude());
        }

        lat = Bits.leftShiftSigned(lat, 32 - bitsEach);
        lon = Bits.leftShiftSigned(lon, 32 - bitsEach);

        bitBuffer.put(0, Bits.toBytes(lat), 0, bitsEach);
        bitBuffer.put(bitsEach, Bits.toBytes(lon), 0, bitsEach);

        return bitBuffer.array();
    }

    public double distanceTo(GPSCoordinatePair other) {
        // Radius of the Earth in kilometers
        final double EARTH_RADIUS = 6371.0;

        // Conversion factor from kilometers to feet
        final double KM_TO_FEET = 3280.84;

        // Convert latitude and longitude from degrees to radians
        double lat1 = Math.toRadians(this.latitude.toDecimal());
        double lon1 = Math.toRadians(this.longitude.toDecimal());

        double lat2 = Math.toRadians(other.latitude.toDecimal());
        double lon2 = Math.toRadians(other.longitude.toDecimal());

        // Calculate the differences
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        // Apply the Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        double distanceInKilometers = EARTH_RADIUS * c;

        // Convert the distance to feet
        return distanceInKilometers * KM_TO_FEET;
    }

    public GPSCoordinatePair deltaTo(GPSCoordinatePair other) {
        return new GPSCoordinatePair(
                new GPSCoordinate(other.latitude().toDecimal() - latitude().toDecimal(), CoordinateType.LATITUDE),
                new GPSCoordinate(other.longitude().toDecimal() - longitude().toDecimal(), CoordinateType.LONGITUDE)
        );
    }

    public int getBytesRequired() {
        int bits = Math.max(this.latitude().getBitsRequiredForStorage(), this.longitude().getBitsRequiredForStorage());
        return (int) Math.ceil(bits * 2D / 8D);
    }

    public GPSCoordinatePair add(GPSCoordinatePair other) {
        return new GPSCoordinatePair(
                new GPSCoordinate(latitude().toDecimal() + other.latitude().toDecimal(), CoordinateType.LATITUDE),
                new GPSCoordinate(longitude().toDecimal() + other.longitude().toDecimal(), CoordinateType.LONGITUDE));
    }

    public GPSCoordinatePair add(double latitude, double longitude) {
        return add(new GPSCoordinatePair(
                new GPSCoordinate(latitude, CoordinateType.LATITUDE),
                new GPSCoordinate(longitude, CoordinateType.LONGITUDE)
        ));
    }


    public GPSCoordinatePair round() {
        return new GPSCoordinatePair(latitude().round(), longitude().round());
    }
}
