package dev.kshl.kshlib.misc;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    public static boolean containsAnyOf(String haystack, String needles) {
        for (char c : needles.toCharArray()) {
            if (haystack.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    public static String repeat(Supplier<Character> supplier, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(supplier.get());
        }
        return out.toString();
    }

    public static char randomCharFrom(Random random, String s) {
        return s.charAt(random.nextInt(s.length()));
    }

    public static ArrayList<String> splitCommasExceptQuotes(String text) {
        ArrayList<String> tokens = new ArrayList<>();
        // Regex pattern to match words outside quotes or within quotes, split at commas
        // and include empty strings for two commas back to back
        Pattern pattern = Pattern.compile("(?:[^,\"]|\"(?:\\\\.|[^\"])*\")+|(?<=,)(?=,)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            // Trim the token to remove leading and trailing whitespace
            String token = matcher.group().trim().replaceAll("(^\")|(\"$)", "").trim();
            // Add the token to the list
            tokens.add(token);
        }

        return tokens;
    }
}
