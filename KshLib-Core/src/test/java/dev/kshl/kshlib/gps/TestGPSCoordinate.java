package dev.kshl.kshlib.gps;

import dev.kshl.kshlib.misc.Bits;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGPSCoordinate {
    @Test
    public void testStringConstructor() {
        GPSCoordinate coordinate = new GPSCoordinate("40°26'46.0\"N");
        assertEquals(40, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(26, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(46.0, coordinate.getDegreeMinuteSecond().seconds());
        assertEquals(Direction.N, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(40.44611111, coordinate.toDecimal(), 0.000001);
        assertEquals("40°26'46.0\"N", coordinate.toString());
    }

    @Test
    public void testDegreeMinuteSecondConstructor() {
        GPSCoordinate coordinate = new GPSCoordinate(40, 26, 46.0, Direction.N);
        assertEquals(40, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(26, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(46.0, coordinate.getDegreeMinuteSecond().seconds());
        assertEquals(Direction.N, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(40.44611111, coordinate.toDecimal(), 0.000001);
        assertEquals("40°26'46.0\"N", coordinate.toString());
    }

    @Test
    public void testDecimalConstructorLatitude() {
        GPSCoordinate coordinate = new GPSCoordinate(40.44611, CoordinateType.LATITUDE);
        assertEquals(40, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(26, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(45.996, coordinate.getDegreeMinuteSecond().seconds(), 0.001);
        assertEquals(Direction.N, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(40.44611, coordinate.toDecimal(), 0.000001);
    }

    @Test
    public void testDecimalConstructorLongitude() {
        GPSCoordinate coordinate = new GPSCoordinate(-79.98223, CoordinateType.LONGITUDE);
        assertEquals(79, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(58, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(56.028, coordinate.getDegreeMinuteSecond().seconds(), 0.001);
        assertEquals(Direction.W, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(-79.98223, coordinate.toDecimal(), 0.000001);
    }

    @Test
    public void testByteArrayConstructorLatitude() {
        GPSCoordinate originalCoordinate = new GPSCoordinate(40.4461128, CoordinateType.LATITUDE);
        byte[] byteArray = originalCoordinate.toByteArray();
        GPSCoordinate coordinate = new GPSCoordinate(byteArray, CoordinateType.LATITUDE);
        assertEquals(40, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(26, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(46.0, coordinate.getDegreeMinuteSecond().seconds(), 0.01);
        assertEquals(Direction.N, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(40.4461128, coordinate.toDecimal(), GPSCoordinate.STORAGE_ERROR);
    }

    @Test
    public void testByteArrayConstructorLongitude() {
        GPSCoordinate originalCoordinate = new GPSCoordinate(-79.98223f, CoordinateType.LONGITUDE);
        byte[] byteArray = originalCoordinate.toByteArray();
        GPSCoordinate coordinate = new GPSCoordinate(byteArray, CoordinateType.LONGITUDE);
        assertEquals(79, coordinate.getDegreeMinuteSecond().degree());
        assertEquals(58, coordinate.getDegreeMinuteSecond().minute());
        assertEquals(56.028f, coordinate.getDegreeMinuteSecond().seconds(), 0.01);
        assertEquals(Direction.W, coordinate.getDegreeMinuteSecond().direction());
        assertEquals(-79.98223f, coordinate.toDecimal(), GPSCoordinate.STORAGE_ERROR);
    }

    @Test
    public void testByteArrayAccuracy() {
        Set<Double> testValues = new HashSet<>(List.of(-180D, 0D, 180D));

        Random random = new Random(0);
        for (int i = 0; i < 100_000; i++) {
            testValues.add(random.nextDouble() * 180);
        }
        for (double coordinateDecimal : testValues) {
            GPSCoordinate originalCoordinate = new GPSCoordinate(coordinateDecimal, CoordinateType.LATITUDE);
            byte[] byteArray = originalCoordinate.toByteArray();

            GPSCoordinate coordinate = new GPSCoordinate(byteArray, CoordinateType.LATITUDE);
            assertEquals(coordinateDecimal, coordinate.toDecimal(), 0.5 / GPSCoordinate.BYTE_STORAGE_POINT);
        }
    }

    @Test
    public void testByteArray() {
        GPSCoordinate latNegativeTiny = new GPSCoordinate(-0.00001, CoordinateType.LATITUDE);
        GPSCoordinate latPositiveTiny = new GPSCoordinate(0.00002, CoordinateType.LATITUDE);
        GPSCoordinate lonNegativeTiny = new GPSCoordinate(-0.00003, CoordinateType.LONGITUDE);
        GPSCoordinate lonPositiveTiny = new GPSCoordinate(0.00004, CoordinateType.LONGITUDE);

        for (int i = 2; i < 6; i++) {
            System.out.println("Testing " + i + " byte array");
            test(latPositiveTiny, lonPositiveTiny, i);
            test(latNegativeTiny, lonNegativeTiny, i);
            test(latNegativeTiny, lonPositiveTiny, i);
            test(latPositiveTiny, lonNegativeTiny, i);
        }

        GPSCoordinate latNegative = new GPSCoordinate(-1.00223, CoordinateType.LATITUDE);
        GPSCoordinate latPositive = new GPSCoordinate(1.00223, CoordinateType.LATITUDE);
        GPSCoordinate lonNegative = new GPSCoordinate(-1.00223, CoordinateType.LONGITUDE);
        GPSCoordinate lonPositive = new GPSCoordinate(1.00223, CoordinateType.LONGITUDE);

        for (int i = 6; i < 8; i++) {
            System.out.println("Testing " + i + " byte array");
            test(latPositive, lonPositive, i);
            test(latNegative, lonNegative, i);
            test(latNegative, lonPositive, i);
            test(latPositive, lonNegative, i);
        }
    }

    @Test
    public void testByteArrayMaximums() {
        GPSCoordinate zeroLat = new GPSCoordinate(0, CoordinateType.LATITUDE);
        GPSCoordinate zeroLon = new GPSCoordinate(0, CoordinateType.LONGITUDE);
        GPSCoordinatePair zero = new GPSCoordinatePair(zeroLat, zeroLon);
        for (int i = 1; i < 8; i++) {
            int max = (int) Math.pow(2, (double) (i * 8) / 2 - 1) - 1;
            int maxNeg = -max - 1;

            GPSCoordinate lat = new GPSCoordinate(max / GPSCoordinate.BYTE_STORAGE_POINT, CoordinateType.LATITUDE);
            GPSCoordinate lon = new GPSCoordinate(maxNeg / GPSCoordinate.BYTE_STORAGE_POINT, CoordinateType.LONGITUDE);

            double distance = zero.distanceTo(new GPSCoordinatePair(lat, zeroLon)) * 2;
            String units = "feet";
            if (distance > 5280) {
                distance /= 5280;
                units = "miles";
            }
            System.out.println(i + " bytes can store " + distance + " " + units + " of information");

            test(lat, lon, i);
        }
    }

    private static void test(GPSCoordinate coordinate1, GPSCoordinate coordinate2, int size) {
        GPSCoordinatePair coordinatePair = new GPSCoordinatePair(coordinate1, coordinate2);
        byte[] encoded = coordinatePair.toByteArray(size);
        GPSCoordinatePair processedPair = GPSCoordinate.fromByteArray(encoded);
        assertEquals(coordinatePair, processedPair);
        System.out.println(coordinatePair + " => " + Bits.toBinaryString(coordinatePair.toByteArray(size)) + " => " + processedPair);
    }

    @Test
    public void testEncodeDecodeSequence() {
        GPSPositionList sequence = new GPSPositionList();
        Random random = new Random(0);

        final double speedAt1MPH = GPSCoordinate.feetToDecimal(1D / 3600 * 15 * 5280);

        long time = System.currentTimeMillis();
        GPSCoordinatePair lastCoordinate = new GPSCoordinatePair(
                new GPSCoordinate(random.nextDouble() * 90, CoordinateType.LATITUDE),
                new GPSCoordinate(random.nextDouble() * 180, CoordinateType.LONGITUDE)
        );
        sequence.add(new GPSTimeAndCoordinate(time, lastCoordinate));


        while (sequence.size() < 50000) {
            lastCoordinate = lastCoordinate.add(
                    random.nextDouble(speedAt1MPH * 20, speedAt1MPH * 85),
                    -random.nextDouble(speedAt1MPH * 20, speedAt1MPH * 85)
            );
            time += 15000L + random.nextLong(-500, 500);
            sequence.add(new GPSTimeAndCoordinate(time, lastCoordinate));
        }
        System.out.println("Sequence travels " + sequence.get(sequence.size() - 1).coordinates().distanceTo(sequence.get(0).coordinates()) + "ft");

        byte[] encoded = GPSCoordinate.encodeCoordinateSequence(sequence);
        GPSPositionList decodedSequence = GPSCoordinate.decodeCoordinateSequence(encoded);

        assertEquals(sequence.size(), decodedSequence.size(), "Mismatched array sizes");

        final long allowedTimeError = 1000;
        final double allowedDistanceError = GPSCoordinate.STORAGE_ERROR * 2;
        System.out.println("Allowed time error: +/-" + allowedTimeError + "ms");
        System.out.println("Allowed distance error: +/-" + GPSCoordinate.decimalToFeet(allowedDistanceError) + "ft, +/-" + allowedDistanceError);

        for (int i = 0; i < decodedSequence.size(); i++) {

            var originalPair = sequence.get(i);
            var decodedPair = decodedSequence.get(i);

            long timeDelta = originalPair.time() - decodedPair.time();
            assert Math.abs(timeDelta) < allowedTimeError : "Mismatched time on #" + i + " by " + timeDelta + "ms";
            assertEquals(originalPair.coordinates().latitude().toDecimal(), decodedPair.coordinates().latitude().toDecimal(), allowedDistanceError, "Mismatched Latitude on #" + i);
            assertEquals(originalPair.coordinates().longitude().toDecimal(), decodedPair.coordinates().longitude().toDecimal(), allowedDistanceError, "Mismatched Longitude on #" + i);

//            System.out.println(i + ". TimeError=" + currentTimeError + ", LatError=" + latError + ", LonError=" + lonError);
        }
        System.out.println("Final actual time error: " + (sequence.get(sequence.size() - 1).time() - decodedSequence.get(decodedSequence.size() - 1).time()) + "ms");
        System.out.println("Final actual distance error: " + sequence.get(sequence.size() - 1).coordinates().distanceTo(decodedSequence.get(decodedSequence.size() - 1).coordinates()) + "ft");

        System.out.println("Encoded bytes: " + encoded.length);
        System.out.println("Number of bytes required without delta encoding: " + (7 + sequence.size() * 8));
    }
}
