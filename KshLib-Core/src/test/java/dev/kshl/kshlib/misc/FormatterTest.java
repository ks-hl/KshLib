package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatterTest {
    @Test
    public void testCapitalizeFirstLetter() {
        assertEquals("Hello There!How Are You?D9That's Good!", Formatter.capitalizeFirstLetters("hello there!hOw are you?d9thAT's good!"));
    }

    @Test
    public void doubleToString() {
        assertEquals("1.000", Formatter.toString(1, 3, false, false));
        assertEquals("1.0", Formatter.toString(1, 3, true, false));
        assertEquals("1.235", Formatter.toString(1.23456789, 3, true, false));
        assertEquals("1", Formatter.toString(1, 3, true, true));
        assertEquals("1.174", Formatter.toString(1.17384723788, 3, true, true));

        assertEquals("0.000", Formatter.toString(0, 3, false, false));
        assertEquals("0", Formatter.toString(0, 3, true, true));
    }

    @Test
    public void testSplit() {
        assertArrayEquals(new String[]{"a", "b", "c"}, Formatter.splitAndInclude("abc", "b"));
        assertArrayEquals(new String[]{"a", "y", "b", "z", "c"}, Formatter.splitAndInclude("aybzc", "[yz]"));
        assertArrayEquals(new String[]{"a", "y", ""}, Formatter.splitAndInclude("ay", "y"));
        assertArrayEquals(new String[]{"", "y", "a"}, Formatter.splitAndInclude("ya", "y"));
        assertArrayEquals(new String[]{"", "a", "", "b", ""}, Formatter.splitAndInclude("ab", "."));
    }

    @Test
    public void testByteToString() {
        assertEquals("1B", Formatter.byteSizeToString(1));
        assertEquals("1KB", Formatter.byteSizeToString(1024));
        assertEquals("1.46KB", Formatter.byteSizeToString(1500));
        assertEquals("1.43MB", Formatter.byteSizeToString(1500000));
    }
}
