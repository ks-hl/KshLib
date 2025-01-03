package dev.kshl.kshlib.misc;


import org.junit.jupiter.api.Test;

public class FloatingMathTest {
    @Test
    public void testFloatingMath() {
        assert FloatingMath.equals(1.1, 1.1);
        assert !FloatingMath.equals(1.1, 1.10001);

        assert FloatingMath.greaterThan(1.0001, 1);
        assert !FloatingMath.greaterThan(1, 1);

        assert FloatingMath.lessThan(1, 1.0001);
        assert !FloatingMath.lessThan(1, 1);

        assert FloatingMath.greaterThanOrEqual(1, 1);
        assert FloatingMath.greaterThanOrEqual(1.001, 1);
        assert !FloatingMath.greaterThanOrEqual(0.999, 1);

        assert FloatingMath.lessThanOrEqual(1, 1);
        assert FloatingMath.lessThanOrEqual(1, 1.001);
        assert !FloatingMath.lessThanOrEqual(1, 0.999);
    }
}
