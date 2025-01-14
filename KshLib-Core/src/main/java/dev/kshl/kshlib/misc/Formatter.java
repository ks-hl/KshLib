package dev.kshl.kshlib.misc;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {
    public static String pluralize(double value, String singular, String plural) {
        if (FloatingMath.equals(Math.abs(value), 1)) return singular;
        return plural;
    }

    public static String possessiveize(String name) {
        if (name.toLowerCase().endsWith("s")) return name + "'";
        return name + "'s";
    }

    private static final Set<String> NO_CAPITALIZE = Set.of("a", "and", "as", "at", "but", "by", "down", "for", "from", "if", "in", "into", "like", "near", "nor", "of", "off", "on", "once", "onto", "or", "over", "past", "so", "than", "that", "to", "upon", "when", "with", "yet");
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z']+");

    public static String capitalizeFirstLetters(String string) {
        char[] chars = string.toLowerCase().toCharArray();
        Matcher matcher = WORD_PATTERN.matcher(string.toLowerCase());
        boolean first = true;
        while (matcher.find()) {
            if (first) first = false;
            else if (NO_CAPITALIZE.contains(matcher.group())) continue;
            chars[matcher.start()] = matcher.group().toUpperCase().charAt(0);
        }
        return String.valueOf(chars);
    }

    public static String capitalizeFirstLettersAll(String string) {
        StringBuilder out = new StringBuilder();
        boolean nonLetter = true;
        for (char c : string.toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                if (nonLetter) out.append(String.valueOf(c).toUpperCase());
                else out.append(String.valueOf(c).toLowerCase());
                nonLetter = false;
                continue;
            }
            if (c != '\'') nonLetter = true;
            out.append(c);
        }
        return out.toString();
    }

    public static String toString(double d, int decimalPlaces, boolean truncateZeroes, boolean truncateDecimal) {
        DecimalFormat df = new DecimalFormat("#." + ("0".repeat(decimalPlaces)));
        String out = df.format(d);
        if (out.startsWith(".")) out = 0 + out;
        else if (out.startsWith("-.")) out = "-0" + out.substring(1);
        if (truncateDecimal) {
            out = out.replaceFirst("\\.?0+$", "");
        } else if (truncateZeroes) {
            out = out.replaceFirst("(?<=\\d)0+$", "");
        }
        return out;
    }

    public static String[] splitAndInclude(String text, String regex) {
        if (regex.isEmpty()) return text.split(regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int index = 0;
        List<String> out = new ArrayList<>();
        while (matcher.find()) {
            out.add(text.substring(index, matcher.start()));
            out.add(matcher.group());
            index = matcher.end();
        }
        out.add(text.substring(index));

        return out.toArray(new String[0]);
    }

    public static String byteSizeToString(final int bytes_) {
        double bytes = bytes_;
        Function<Double, String> round = d -> toString(d, 2, true, true);
        if (bytes < 1024) return round.apply(bytes) + "B";
        if ((bytes /= 1024) < 1024) return round.apply(bytes) + "KB";
        if ((bytes /= 1024) < 1024) return round.apply(bytes) + "MB";
        if ((bytes /= 1024) < 1024) return round.apply(bytes) + "GB";
        if ((bytes /= 1024) < 1024) return round.apply(bytes) + "TB";
        return round.apply(bytes / 1024) + "PB";
    }

    /**
     * Converts a number into a comma-separated number by thousands. i.e. 10000 -> "10,000"
     *
     * @param number The number to convert
     * @return The converted string
     * @see NumberFormat#format(long)
     * @see Locale#US
     */
    public static String formatThousands(long number) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        return formatter.format(number);
    }

    /**
     * Creates a string composed of blocks, adding up to the specified number, in fractions of 8. For instance, if '8' is provided, this will return one full block. If '4' is provided, this will return one half block.
     *
     * @param minSizeBlock The smallest block to use. Blocks are 1, 2, 4, 5, and 9.
     * @param maxSizeBlock The largest block to use. Blocks are 1, 2, 4, 5, and 9.
     */
    public static String getBlocks(int blocks, boolean startFromLeft, int minSizeBlock, int maxSizeBlock) {
        StringBuilder out = new StringBuilder();
        Consumer<String> append = s -> {
            if (startFromLeft) {
                out.append(s);
            } else {
                out.insert(0, s);
            }
        };
        if (9 >= minSizeBlock && 9 <= maxSizeBlock)
            while (blocks >= 9) {
                append.accept(Characters.BAR_9);
                blocks -= 9;
            }
        if (5 >= minSizeBlock && 5 <= maxSizeBlock)
            while (blocks >= 5) {
                append.accept(Characters.BAR_5);
                blocks -= 5;
            }
        if (4 >= minSizeBlock && 4 <= maxSizeBlock)
            while (blocks >= 4) {
                append.accept(Characters.BAR_4);
                blocks -= 4;
            }
        if (2 >= minSizeBlock && 2 <= maxSizeBlock)
            while (blocks >= 2) {
                append.accept(Characters.BAR_2);
                blocks -= 2;
            }
        if (1 >= minSizeBlock && 1 <= maxSizeBlock)
            while (blocks >= 1) {
                append.accept(Characters.BAR_1);
                blocks -= 1;
            }
        return out.toString();
    }
}
