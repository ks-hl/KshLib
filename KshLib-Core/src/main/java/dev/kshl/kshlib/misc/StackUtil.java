package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;

import java.lang.annotation.Retention;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class StackUtil {
    public static String dumpThreadStack(Predicate<String> check) {
        StringBuilder trace = new StringBuilder();
        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            if (entry.getValue().length == 0 || !entry.getKey().isAlive()) {
                continue;
            }

            String thread = "Thread #" + entry.getKey().getId() + ":";
            thread += format(entry.getValue(), 20);

            if (!check.test(thread)) continue;

            if (!trace.isEmpty()) {
                trace.append("\n\n");
            }
            trace.append(thread);
        }
        return trace.toString();
    }

    public static String format(@Nonnull Throwable t, int lineLimit) {
        String out = String.format("%s: %s", t.getClass().getName(), t.getMessage());

        out += format(t.getStackTrace(), lineLimit);
        return out;
    }

    public static String format(@Nonnull StackTraceElement[] stack, int linelimit) {
        if (linelimit == 0) linelimit = Integer.MAX_VALUE;
        StringBuilder out = new StringBuilder();
        Arrays.stream(stack).limit(linelimit).forEach((s) -> {
            out.append("\n");
            out.append(format(s));
        });
        if (linelimit < stack.length) {
            out.append("\n    [").append(stack.length - linelimit).append(" more lines]");
        }
        return out.toString();
    }

    public static String format(StackTraceElement element) {
        return String.format("    at %s.%s(%s:%s)", element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber());
    }
}