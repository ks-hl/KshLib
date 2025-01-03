package dev.kshl.kshlib.vector;

public class VectorMath {

    public static Vector toVector(double pitch, double yaw, boolean radians) {
        if (!radians) {
            pitch = Math.toRadians(pitch);
            yaw = Math.toRadians(yaw);
        }

        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        return new Vector(x, y, z);
    }

    public static double normalizeAngle180(double angle) {
        angle = angle % 360; // Reduce to [-360, 360] range
        if (angle > 180) angle -= 360; // Shift to [-180, 180] range
        else if (angle < -180) angle += 360; // Shift to [-180, 180] range
        return angle;
    }

    public static double normalizeAngle90(double angle) {
        angle = angle % 180; // Reduce to [-180, 180] range
        if (angle > 90) angle -= 180; // Shift to [-90, 90] range
        else if (angle < -90) angle += 180; // Shift to [-90, 90] range
        return angle;
    }

    static boolean equals(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }
}
