package dev.kshl.kshlib.misc;

import java.util.function.UnaryOperator;

public class FloatingMath {
    public static final double EPSILON = 1E-6;

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    /**
     * @return Whether `a` is > `b`
     */
    public static boolean greaterThan(double a, double b) {
        return a > b + EPSILON;
    }

    /**
     * @return Whether `a` is < `b`
     */
    public static boolean lessThan(double a, double b) {
        return a < b - EPSILON;
    }

    /**
     * @return Whether `a` is > `b`
     */
    public static boolean greaterThanOrEqual(double a, double b) {
        return a > b - EPSILON;
    }

    /**
     * @return Whether `a` is < `b`
     */
    public static boolean lessThanOrEqual(double a, double b) {
        return a < b + EPSILON;
    }

    public static double withSignificance(double d, UnaryOperator<Double> operation, int significance) {
        int pow10 = pow10(significance);
        return operation.apply(d * pow10) / pow10;
    }

    public static int pow10(int pow) {
        int out = 1;
        for (int i = 0; i < pow; i++) {
            out *= 10;
        }
        return out;
    }
}
