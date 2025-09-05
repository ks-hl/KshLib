package dev.kshl.kshlib.misc;

public class MathUtil {
    public static int sq(int i) {
        return i * i;
    }

    public static int pow(int base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("Exponent must be >= 0");
        }
        if (exponent > 1000000) {
            return (int) Math.pow(base, exponent);
        }
        int out = 1;
        for (int i = 0; i < exponent; i++) {
            out *= base;
        }
        return out;
    }

    public static double sq(double d) {
        return d * d;
    }

    public static double distance2DSq(double x1, double y1, double x2, double y2) {
        return sq(x1 - x2) + sq(y1 - y2);
    }

    public static double distance2D(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distance2DSq(x1, y1, x2, y2));
    }
}
