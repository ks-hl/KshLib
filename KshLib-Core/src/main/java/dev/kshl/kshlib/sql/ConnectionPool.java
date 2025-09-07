package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class ConnectionPool {
    @Getter
    private boolean closing;
    private final Map<Long, Long> usage = new HashMap<>();

    protected ConnectionPool() throws ClassNotFoundException {
        checkDriver();
    }

    protected boolean testConnection(Connection connection) {
        try {
            return connection.isValid(2);
        } catch (SQLException ignored) {
            return false;
        }
    }

    public void consume(ConnectionConsumer connectionConsumer, long wait) throws SQLException, BusyException {
        function(connection -> {
            connectionConsumer.accept(connection);
            return null;
        }, wait);
    }

    public final <T> T function(ConnectionFunction<T> connectionFunction, long wait) throws SQLException, BusyException {
        try {
            return executeWithException(connectionFunction::apply, wait);
        } catch (SQLException | BusyException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract <T> T executeWithException_(ConnectionFunctionWithException<T> task, long wait) throws Exception;

    public <T> T executeWithException(ConnectionFunctionWithException<T> task, long wait) throws Exception {
        return executeWithException_(connection -> {
            long start = nanoTime();
            synchronized (usage) {
                usage.put(start, null);
            }
            try {
                return task.apply(connection);
            } finally {
                long end = nanoTime();
                synchronized (usage) {
                    usage.put(start, end);
                    usage.values().removeIf(p -> p != null && end - p > TimeUnit.MINUTES.toNanos(30));
                }
            }
        }, wait);
    }

    /**
     * The ratio of time this pool has been actively accessing the database in the past [5 minutes], [1 minute], and [5 seconds]
     * Note: This may be >1 if there are multiple Connections
     */
    public double[] getUsageTimeRatios() {
        synchronized (usage) {
            return new double[]{
                    calculateUsage(5, TimeUnit.MINUTES),
                    calculateUsage(1, TimeUnit.MINUTES),
                    calculateUsage(5, TimeUnit.SECONDS)
            };
        }
    }

    private double calculateUsage(long duration, TimeUnit timeUnit) {
        long durationNanos = timeUnit.toNanos(duration);
        long now = nanoTime();
        long startWindow = now - durationNanos;
        long total = 0;
        synchronized (usage) {
            for (Map.Entry<Long, Long> entry : usage.entrySet()) {
                long start = entry.getKey();
                long end = Optional.ofNullable(entry.getValue()).orElse(now);
                if (end < startWindow) continue;
                total += end - Math.max(start, startWindow);
            }
        }
        return total / (double) durationNanos;
    }

    private long lastNanoTime;
    private final Object nanoTimeLock = new Object();

    private long nanoTime() {
        synchronized (nanoTimeLock) {
            long time = System.nanoTime();
            if (time > lastNanoTime) {
                return lastNanoTime = time;
            }
            return ++lastNanoTime;
        }
    }

    public final boolean isMySQL() {
        return this instanceof ConnectionPoolMySQL;
    }

    @SuppressWarnings("unused")
    public abstract int getActiveConnections();

    @SuppressWarnings("unused")
    public abstract int getAllEstablishedConnections();

    public final void close() {
        if (closing) return;
        closing = true;
        closeInternal();
    }

    protected abstract void closeInternal();

    protected abstract void checkDriver() throws ClassNotFoundException;
}
