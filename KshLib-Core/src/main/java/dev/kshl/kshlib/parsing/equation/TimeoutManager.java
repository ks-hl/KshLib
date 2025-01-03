package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.parsing.equation.exception.TimeoutException;

import java.util.concurrent.TimeUnit;

public class TimeoutManager {
    private final long timeout;
    private final long start = System.currentTimeMillis();

    public TimeoutManager(long timeout, TimeUnit timeUnit) {
        this.timeout = timeUnit.toMillis(timeout);
    }

    public synchronized void checkIn() {
        if (Thread.interrupted() || System.currentTimeMillis() - start > timeout) {
            throw new TimeoutException();
        }
    }
}
