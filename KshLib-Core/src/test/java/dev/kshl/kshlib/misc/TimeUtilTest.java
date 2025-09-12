package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static dev.kshl.kshlib.misc.TimeUtil.DAY_MILLIS;
import static dev.kshl.kshlib.misc.TimeUtil.HOUR_MILLIS;
import static dev.kshl.kshlib.misc.TimeUtil.MINUTE_MILLIS;
import static dev.kshl.kshlib.misc.TimeUtil.SECOND_MILLIS;
import static dev.kshl.kshlib.misc.TimeUtil.millisToStringExtended;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TimeUtilTest {

    @Test
    public void testMillisToStringLessThanSecond() {
        assertEquals("1ms", TimeUtil.millisToString(1));
        assertEquals("999ms", TimeUtil.millisToString(999));

        assertEquals("999µs", TimeUtil.millisToString(0.999));
        assertEquals("1µs", TimeUtil.millisToString(0.001));
        assertEquals("1.11µs", TimeUtil.millisToString(0.001_111));

        assertEquals("999ns", TimeUtil.millisToString(0.000_999));

        assertEquals("1.69ps", TimeUtil.millisToString(0.000_000_001689));
    }

    @Test
    public void testMillisToStringMoreThanSecond() {
        assertEquals("1s", TimeUtil.millisToString(1_000));
        assertEquals("59s", TimeUtil.millisToString(59_000));

        assertEquals("1m", TimeUtil.millisToString(60_000));
        assertEquals("5m", TimeUtil.millisToString(299_999));
        assertEquals("5m", TimeUtil.millisToString(5 * 60_000));
        assertEquals("5.11m", TimeUtil.millisToString(5.11 * 60_000));
        assertEquals("5.12m", TimeUtil.millisToString(5.119 * 60_000));
    }

    @Test
    public void testStringToMillis() {
        // Test simple values
        assertEquals(SECOND_MILLIS, TimeUtil.stringToMillis("1s"));
        assertEquals(MINUTE_MILLIS, TimeUtil.stringToMillis("1m"));
        assertEquals(HOUR_MILLIS, TimeUtil.stringToMillis("1h"));
        assertEquals(DAY_MILLIS, TimeUtil.stringToMillis("1d"));

        // Test combined values
        assertEquals(SECOND_MILLIS * 90, TimeUtil.stringToMillis("1m 30s"));
        assertEquals(MINUTE_MILLIS * 90, TimeUtil.stringToMillis("1h 30m"));
        assertEquals(MINUTE_MILLIS * 90 + SECOND_MILLIS * 45, TimeUtil.stringToMillis("1h 30m 45s"));
        assertEquals(DAY_MILLIS + 12 * HOUR_MILLIS + 30 * MINUTE_MILLIS + 45 * SECOND_MILLIS, TimeUtil.stringToMillis("1d 12h 30m 45s"));

        // Test out of order
        assertEquals(DAY_MILLIS + 12 * HOUR_MILLIS + 30 * MINUTE_MILLIS + 45 * SECOND_MILLIS, TimeUtil.stringToMillis("45s 12h 1d 30m"));

        // Test floating point values
        assertEquals(1.501 * SECOND_MILLIS, TimeUtil.stringToMillis("1.500911111111111111111s"));
        assertEquals(1.5 * MINUTE_MILLIS, TimeUtil.stringToMillis("1.5m"));
        assertEquals(1.5 * HOUR_MILLIS, TimeUtil.stringToMillis("1.5h"));
        assertEquals(1.589470, TimeUtil.stringToMillis("1.589470d") / (double) DAY_MILLIS);

        // Test invalid characters
        assertThrows(NumberFormatException.class, () -> TimeUtil.stringToMillis("1x"));
        assertThrows(NumberFormatException.class, () -> TimeUtil.stringToMillis("1y"));
        assertThrows(NumberFormatException.class, () -> TimeUtil.stringToMillis("1z"));
        assertThrows(NumberFormatException.class, () -> TimeUtil.stringToMillis("1"));

        // Test edge cases
        assertEquals(0, TimeUtil.stringToMillis("0s"));
        assertEquals(123457, TimeUtil.stringToMillis("123.456789s"));
        assertEquals(86400000, TimeUtil.stringToMillis("1d 0h 0m 0s"));

        // Test 'e' suffix
        assertEquals(123456789, TimeUtil.stringToMillis("123456789e"));

        // Test 'ms' suffix
        assertEquals(123456789, TimeUtil.stringToMillis("123456789ms"));
    }

    @Test
    public void testMillisToStringExtended() {
        // Test 0 milliseconds
        assertEquals("0s", millisToStringExtended(0));
        assertEquals("0ms", millisToStringExtended(0, true));

        // Test milliseconds
        assertEquals("0s", millisToStringExtended(5));
        assertEquals("0s", millisToStringExtended(499));
        assertEquals("1s", millisToStringExtended(500));
        assertEquals("5ms", millisToStringExtended(5, true));
        assertEquals("1s", millisToStringExtended(999));
        assertEquals("999ms", millisToStringExtended(999, true));

        // Test less than a minute
        assertEquals("1s", millisToStringExtended(millis(1)));
        assertEquals("59s", millisToStringExtended(millis(59)));

        // Test exactly one minute
        assertEquals("1m", millisToStringExtended(millis(60)));

        // Test minutes and seconds
        assertEquals("1m 30s", millisToStringExtended(millis(1, 30)));
        assertEquals("2m 45s", millisToStringExtended(millis(2, 45)));

        // Test exactly one hour
        assertEquals("1h", millisToStringExtended(millis(1, 0, 0)));

        // Test hours and minutes
        assertEquals("1h 30m", millisToStringExtended(millis(1, 30, 0)));
        assertEquals("2h 45m", millisToStringExtended(millis(2, 45, 0)));

        // Test hours, minutes, and seconds
        assertEquals("1h 30m 45s", millisToStringExtended(millis(1, 30, 45)));
        assertEquals("2h 45m 30s", millisToStringExtended(millis(2, 45, 30)));

        // Test exactly one day
        assertEquals("1d", millisToStringExtended(DAY_MILLIS));

        // Test days and hours
        assertEquals("1d 12h", millisToStringExtended(DAY_MILLIS + 12 * HOUR_MILLIS));
        assertEquals("2d 3h", millisToStringExtended(2 * DAY_MILLIS + 3 * HOUR_MILLIS));

        // Test days, hours, minutes, and seconds
        assertEquals("1d 12h 30m 45s", millisToStringExtended(DAY_MILLIS + millis(12, 30, 45)));
        assertEquals("2d 3h 45m 30s", millisToStringExtended(2 * DAY_MILLIS + millis(3, 45, 30)));

        // Test large values
        assertEquals(Integer.MAX_VALUE + "d 12h 30m 45s", millisToStringExtended(Integer.MAX_VALUE * DAY_MILLIS + millis(12, 30, 45)));
        assertEquals("106751991167d 7h 12m 56s", millisToStringExtended(Long.MAX_VALUE, false));
        assertEquals("106751991167d 7h 12m 55s 807ms", millisToStringExtended(Long.MAX_VALUE, true));

        assertEquals("5m", millisToStringExtended(299999));
        assertEquals("5h", millisToStringExtended(5 * 3600000L - 1));
    }

    private static long millis(long seconds) {
        return seconds * SECOND_MILLIS;
    }

    private static long millis(long minutes, long seconds) {
        return minutes * MINUTE_MILLIS + millis(seconds);
    }

    private static long millis(long hours, long minutes, long seconds) {
        return hours * HOUR_MILLIS + millis(minutes, seconds);
    }

    private static long millis(long days, long hours, long minutes, long seconds) {
        return days * DAY_MILLIS + millis(hours, minutes, seconds);
    }
}
