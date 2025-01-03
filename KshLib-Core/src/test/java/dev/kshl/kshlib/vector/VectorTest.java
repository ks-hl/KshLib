package dev.kshl.kshlib.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorTest {

    @Test
    void testCrossProduct() {
        Vector v1 = new Vector(1, 0, 0);
        Vector v2 = new Vector(0, 1, 0);
        Vector result = v1.crossProduct(v2);

        assertEquals(0, result.getX(), 1e-7);
        assertEquals(0, result.getY(), 1e-7);
        assertEquals(1, result.getZ(), 1e-7);
    }

    @Test
    void testDotProduct() {
        Vector v1 = new Vector(1, 2, 3);
        Vector v2 = new Vector(4, -5, 6);
        double result = v1.dotProduct(v2);

        assertEquals(12, result, 1e-7);
    }

    @Test
    void testRotate() {
        Vector v = new Vector(1, 0, 0);
        Vector axis = new Vector(0, 0, 1);
        double angle = Math.PI / 2; // 90 degrees

        Vector result = v.rotate(axis, angle);

        assertEquals(0, result.getX(), 1e-7);
        assertEquals(1, result.getY(), 1e-7);
        assertEquals(0, result.getZ(), 1e-7);
    }

    @Test
    void testRotateWithNonUnitAxis() {
        Vector v = new Vector(1, 0, 0);
        Vector axis = new Vector(0, 0, 10); // Non-unit axis
        double angle = Math.PI / 2; // 90 degrees

        Vector result = v.rotate(axis, angle);

        assertEquals(0, result.getX(), 1e-7);
        assertEquals(1, result.getY(), 1e-7);
        assertEquals(0, result.getZ(), 1e-7);
    }

    @Test
    void testNormalize() {
        Vector v = new Vector(3, 4, 0);
        v.normalize();

        assertEquals(1, v.length(), 1e-7);
    }

    @Test
    void testLength() {
        Vector v = new Vector(3, 4, 0);
        double length = v.length();

        assertEquals(5, length, 1e-7);
    }



    @Test
    void testRotationOfFutureVectorWithCorrectExpectedValues() {
        // Given stationary and expected vectors with realistic accelerometer values
        Vector V0 = new Vector(0.58, -0.28, 0.76); // Example stationary vector
        Vector V_expected = new Vector(0.0, 0.0, 1.0); // Expected orientation (aligned with Z-axis)

        // Calculate rotation axis and angle
        Vector axis = V0.crossProduct(V_expected);
        double angle = Math.acos(V0.dotProduct(V_expected) / (V0.length() * V_expected.length()));

        // Rotate future vector with realistic values
        Vector V_future = new Vector(0.35, 0.48, 0.80); // Example future vector
        Vector V_rotated = V_future.rotate(axis, angle);

        // Independently calculated expected values (using manual calculation or another reliable tool)
        double expectedX = -0.1385; // Correctly calculated expected X value
        double expectedY = 0.7158;  // Correctly calculated expected Y value
        double expectedZ = 0.6792;  // Correctly calculated expected Z value

        // Assert the rotated vector against expected values
        assertEquals(expectedX, V_rotated.getX(), 1e-4);
        assertEquals(expectedY, V_rotated.getY(), 1e-4);
        assertEquals(expectedZ, V_rotated.getZ(), 1e-4);
    }
}
