package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestRomanNumber {
    private static final Map<String, Integer> expected = new HashMap<>();

    static {
        expected.put("II", 2);
        expected.put("III", 3);
        expected.put("VI", 6);
        expected.put("VII", 7);
        expected.put("VIII", 8);
        expected.put("IX", 9);
        expected.put("X", 10);

        // Tens
        expected.put("XX", 20);
        expected.put("XXX", 30);
        expected.put("XL", 40);
        expected.put("L", 50);
        expected.put("LX", 60);
        expected.put("LXX", 70);
        expected.put("LXXX", 80);
        expected.put("XC", 90);
        expected.put("C", 100);

        // Hundreds
        expected.put("CC", 200);
        expected.put("CCC", 300);
        expected.put("CD", 400);
        expected.put("D", 500);
        expected.put("DC", 600);
        expected.put("DCC", 700);
        expected.put("DCCC", 800);
        expected.put("CM", 900);
        expected.put("M", 1000);

        // Mixed combinations
        expected.put("XIV", 14);
        expected.put("XXIX", 29);
        expected.put("XLII", 42);
        expected.put("LXXXVIII", 88);
        expected.put("XCIX", 99);
        expected.put("CXXIII", 123);
        expected.put("CCXLVI", 246);
        expected.put("DCCCLXXXVIII", 888);
        expected.put("CMXCIX", 999);

        // Larger numbers
        expected.put("MDCLXVI", 1666);
        expected.put("MMXIV", 2014);
        expected.put("MMXXI", 2021);
        expected.put("MMMCMXCIX", 3999);
        for (Map.Entry<Integer, String> entry : RomanNumber.getMap().entrySet()) {
            expected.put(entry.getValue(), entry.getKey());
        }
    }

    @Test
    public void testToRoman() {
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            try {
                assertEquals(entry.getKey(), RomanNumber.toRoman(entry.getValue()), "Invalid output for " + entry.getValue());
            } catch (Throwable t) {
                System.err.println("Error occurred processing '" + entry.getValue() + "'");
                throw t;
            }
        }
    }

    @Test
    public void testToInt() {
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            try {
                assertEquals(entry.getValue(), RomanNumber.toInt(entry.getKey()), "Invalid output for " + entry.getKey());
            } catch (Throwable t) {
                System.err.println("Error occurred processing '" + entry.getKey() + "'");
                throw t;
            }
        }
    }

    @Test
    public void testToIntInvalid() {
        // Single invalid characters
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("x"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("y"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("!"));

        // Invalid sequences
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IIV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IXX"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IL"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IC"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("ID"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IM"));

        // Invalid combinations
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IIII"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("VV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("XXXX"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("LL"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("CCCC"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("DD"));

        // Mixed valid and invalid characters
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IXXV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("MDCLXYVI"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("MCMXCIXY"));

        // Empty string
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt(""));

        // Whitespace and empty strings
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt(" "));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("\t"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("\n"));

        // Trailing invalid characters
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("XIXY"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("XXIY"));

        // Leading invalid characters
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("YXIX"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("YXXI"));

        // Repeated invalid sequences
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IIIIII"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("VVV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("XXXXX"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("LLLL"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("CCCCCC"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("DDDD"));

        // Invalid sequences with valid parts
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IIIV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IXXV"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("ILX"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("ICL"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IDL"));
        assertThrows(IllegalArgumentException.class, () -> RomanNumber.toInt("IML"));
    }
}
