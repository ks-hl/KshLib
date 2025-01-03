package dev.kshl.kshlib.parsing;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CommandUtil {
    public static final Pattern PATTERN_KEY_VALUE = Pattern.compile("[\\w-]+:.+");
    public static final Pattern PATTERN_FLAG = Pattern.compile("#(\\w+)");

    public static String concat(String[] args, int i) {
        if (args.length == 0) return "";
        StringBuilder out = new StringBuilder(args[i++]);
        for (; i < args.length; i++) {
            out.append(" ").append(args[i]);
        }
        return out.toString();
    }

    public static String concat(String[] args) {
        return concat(args, 0);
    }

    /**
     * Parses the provided arguments by iterating them all and splitting each argument by spaces, and iterating those.<br>
     * <br>
     * With each group of non-space characters, it will first attempt to match a flag with the pattern `#(\w+)`
     * If that does not match, it will then testPassword if it is a key:value argument with the pattern `[\w-]+:.+`.
     * If that matches, it will continue to add arguments to the `value` unless another key:value or #flag is found.
     * If an argument is found which does not match either pattern and there was no preceding key:value pair
     * (i.e. it is the start of the string, or it is succeeding a #flag), it will be added to the ignored list.
     */
    public static KeyValueResult parseKeyValue(@Nonnull String... args) {
        //noinspection ConstantValue
        if (args == null || args.length == 0) throw new IllegalArgumentException("Null or empty arguments");
        LinkedHashMap<String, String> keyValues = new LinkedHashMap<>();
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        ArrayList<String> ignored = new ArrayList<>();
        StringBuilder building = new StringBuilder();
        Runnable commitBuild = () -> {
            if (building.isEmpty()) return;
            int colonIndex = building.indexOf(":");
            keyValues.put(building.substring(0, colonIndex), building.substring(colonIndex + 1));
            building.setLength(0);
        };
        for (String arg : args) {
            for (String arg_ : arg.split(" ")) {
                if (arg_.isEmpty()) continue;
                Matcher matcher = PATTERN_FLAG.matcher(arg_);
                if (matcher.find()) {
                    commitBuild.run();
                    flags.add(matcher.group(1));
                    continue;
                }
                if (PATTERN_KEY_VALUE.matcher(arg_).matches()) {
                    commitBuild.run();
                } else if (!building.isEmpty()) {
                    building.append(" ");
                } else {
                    ignored.add(arg_);
                    continue;
                }
                building.append(arg_);
            }
        }
        commitBuild.run();
        return new KeyValueResult(keyValues, flags, ignored);
    }

    public static String skipFirstArg(String command) {
        if (!command.contains(" ")) return "";
        return command.substring(command.indexOf(" ")).trim();
    }

    public record KeyValueResult(LinkedHashMap<String, String> keyValues, LinkedHashSet<String> flags,
                                 ArrayList<String> ignored) {
    }

    private static <C extends Collection<String>> C filter(C c, Predicate<String> filter, Collector<String, ?, C> collector) {
        return c.stream().filter(filter).collect(collector);
    }

    private static <C extends Collection<String>> C filterStartsWith(C c, String filter, boolean caseSensitive, Collector<String, ?, C> collector) {
        return filter(c, s -> {
            if (caseSensitive) {
                return s.startsWith(filter);
            } else {
                return s.toLowerCase().startsWith(filter.toLowerCase());
            }
        }, collector);
    }

    private static <C extends Collection<String>> C filterContains(C c, String filter, boolean caseSensitive, Collector<String, ?, C> collector) {
        return filter(c, s -> {
            if (caseSensitive) {
                return s.contains(filter);
            } else {
                return s.toLowerCase().contains(filter.toLowerCase());
            }
        }, collector);
    }

    public static List<String> filterStartsWith(List<String> list, String filter, boolean caseSensitive) {
        return filterStartsWith(list, filter, caseSensitive, Collectors.toList());
    }

    public static List<String> filterContains(List<String> list, String filter, boolean caseSensitive) {
        return filterContains(list, filter, caseSensitive, Collectors.toList());
    }

    public static Set<String> filterStartsWith(Set<String> set, String filter, boolean caseSensitive) {
        return filterStartsWith(set, filter, caseSensitive, Collectors.toSet());
    }

    public static Set<String> filterContains(Set<String> set, String filter, boolean caseSensitive) {
        return filterContains(set, filter, caseSensitive, Collectors.toSet());
    }

}
