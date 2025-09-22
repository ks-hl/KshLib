package dev.kshl.kshlib.exceptions;

import dev.kshl.kshlib.misc.StackUtil;

import javax.annotation.Nullable;

public class BusyException extends Exception {
    public BusyException(@Nullable StackTraceElement[] stack, long threadID, long heldSince) {
        this("Resource busy, currently held by " + (threadID < 0 ? "none" : ("Thread #" + threadID)) + ", held for " + (System.currentTimeMillis() - heldSince) + "ms" + //
                (stack != null ? ("\n" + StackUtil.format(stack, 50) + "\n==== End held by ====\n") : ""));
    }

    public BusyException() {
        this("Resource busy.");
    }

    public BusyException(String message) {
        super(message);
    }
}
