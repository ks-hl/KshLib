package dev.kshl.kshlib.misc;

import java.util.Arrays;

public class StackTrace {
    private final StackTraceElement[] elements;

    public StackTrace(StackTraceElement[] elements) {
        this.elements = elements;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StackTrace trace)) return false;
        return Arrays.equals(elements, trace.elements);
    }

    @Override
    public String toString() {
        return toString(100);
    }

    public String toString(int lineLimit) {
        return StackUtil.format(elements, lineLimit);
    }
}
