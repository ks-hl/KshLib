package dev.kshl.kshlib.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftFormatter {
    private static final String colorCharPattern = "[" + ChatColor.COLOR_CHAR + "&]";
    private static final Pattern formattedHexPattern = Pattern.compile(colorCharPattern + "x" + (colorCharPattern + "([0-9a-f])").repeat(6));
    private static final Pattern rawHexPattern = Pattern.compile(colorCharPattern + "#" + "([0-9a-f])".repeat(6));
    private static final Function<MatchResult, String> hexReplacer = s -> ChatColor.of("#" + s.group(1) + s.group(2) + s.group(3) + s.group(4) + s.group(5) + s.group(6)).toString();
    private static final Pattern formattingPattern = Pattern.compile(colorCharPattern + "([lmno])");
    private static final Pattern magicPattern = Pattern.compile(colorCharPattern + "(k)");
    private static final Pattern basicColorPattern = Pattern.compile(colorCharPattern + "([0-9a-f])");
    private static final Function<MatchResult, String> colorReplacer = s -> ChatColor.COLOR_CHAR + s.group(1);

    public static String replaceHex(String msg) {
        msg = replace(msg, formattedHexPattern, hexReplacer);
        return replace(msg, rawHexPattern, hexReplacer);
    }

    public static String replaceFormatting(String msg) {
        return replace(msg, formattingPattern, colorReplacer);
    }

    public static String replaceMagic(String msg) {
        return replace(msg, magicPattern, colorReplacer);
    }

    public static String replaceBasicColor(String msg) {
        return replace(msg, basicColorPattern, colorReplacer);
    }

    private static String replace(String msg, Pattern pattern, Function<MatchResult, String> replacer) {
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) msg = matcher.replaceAll(replacer);
        return msg;
    }

    public static BaseComponent[] concat(BaseComponent[] a, BaseComponent[] b) {
        BaseComponent[] out = new BaseComponent[a.length + b.length];
        for (int i = 0; i < out.length; i++) {
            if (i < a.length) {
                out[i] = a[i];
            } else {
                out[i] = b[i - a.length];
            }
        }
        return out;
    }
}
