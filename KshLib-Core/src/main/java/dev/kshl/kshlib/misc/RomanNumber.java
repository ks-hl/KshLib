package dev.kshl.kshlib.misc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

// Credit: https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java
public class RomanNumber {

    private final static TreeMap<Integer, String> map = new TreeMap<>();

    static {
        map.put(1000, "M");
        map.put(900, "CM");
        map.put(500, "D");
        map.put(400, "CD");
        map.put(100, "C");
        map.put(90, "XC");
        map.put(50, "L");
        map.put(40, "XL");
        map.put(10, "X");
        map.put(9, "IX");
        map.put(5, "V");
        map.put(4, "IV");
        map.put(1, "I");
    }

    public static Map<Integer, String> getMap() {
        return Collections.unmodifiableMap(map);
    }

    public static String toRoman(int number) {
        boolean neg = false;
        if (number < 0) {
            neg = true;
            number *= -1;
        }
        int l = map.floorKey(number);

        String out = (neg ? "-" : "") + map.get(l);
        if (number > l) {
            out += toRoman(number - l);
        }
        return out;
    }

    public static int toInt(String roman) {
        return new Parser(roman).parse();
    }

    private static class Parser extends GenericParser {
        private int lastFound;

        public Parser(String text) {
            super(text, false);

            if (text.isBlank()) {
                throw new IllegalArgumentException("Empty string");
            }
        }

        @Override
        public void init() {
            super.init();
            lastFound = Integer.MAX_VALUE;
        }

        public int parse() {
            init();
            boolean negative = eat('-');
            int val = 0;
            while (ch >= 0) {
                val += parseNumeral();
            }
            if (negative) val *= -1;
            return val;
        }

        public int parseNumeral() {
            for (Map.Entry<Integer, String> entry : map.descendingMap().entrySet()) {
                int val = 0;
                while (eat(entry.getValue())) {
                    if (entry.getKey() > lastFound) {
                        throw new IllegalArgumentException("Invalid sequence. " + entry.getValue() + " after " + map.get(lastFound));
                    }
                    lastFound = entry.getKey();
                    val += entry.getKey();
                }
                if (val == 0) continue;

                String shouldHaveUsed = Optional.ofNullable(map.floorEntry(val)).map(Map.Entry::getValue).orElse(null);
                if (!entry.getValue().equals(shouldHaveUsed)) {
                    throw new IllegalArgumentException("Invalid character. Multiple '" + entry.getValue() + "' used instead of '" + shouldHaveUsed + "'");
                }
                return val;
            }

            throw new IllegalArgumentException(getUnexpectedCharacterMessage());
        }
    }
}