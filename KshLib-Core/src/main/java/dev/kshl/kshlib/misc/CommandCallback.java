package dev.kshl.kshlib.misc;

import dev.kshl.kshlib.log.ILogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandCallback<T> {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    private final Map<UUID, Callback> callbacks = new ConcurrentHashMap<>();
    private final ILogger logger;

    public CommandCallback(ILogger logger) {
        this.logger = logger;
    }

    /**
     * @param sender       The sender that must be equal when the callback is called
     * @param onExecute    What to do when the callback is executed
     * @param oneTime      Whether to restrict this callback to a single use
     * @param ttl          A TTL between 0 (exclusive) and 1 hour (inclusive)
     * @param ttlTimeUnit  The units of the TTL
     * @param onExpiration What to do when the callback expires, or null
     * @return The UUID that should be in the command and returned to handleCallback as the uuidString
     */
    public UUID registerCallback(T sender, Runnable onExecute, boolean oneTime, long ttl, TimeUnit ttlTimeUnit, @Nullable Runnable onExpiration) {
        long ttlMillis = ttlTimeUnit.toMillis(ttl);
        if (ttlMillis <= 0 || ttlMillis > TimeUnit.HOURS.toMillis(1)) {
            throw new IllegalArgumentException("TTL must be between 0 and 1 hour");
        }

        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();

        Callback cb = new Callback(uuid, now, now + ttlMillis, sender, onExecute, oneTime);
        callbacks.put(uuid, cb);

        executor.schedule(() -> {
            // Remove from the map; if already removed, nothing to do.
            Callback removed = callbacks.remove(uuid);
            if (removed == null) return;

            // Make sure expiration action runs only once.
            if (!removed.markExpired()) return;

            if (onExpiration != null) {
                safeRun(uuid, onExpiration);
            }
        }, ttl, ttlTimeUnit);

        return uuid;
    }

    /**
     * Execute the callback if all conditions are met
     *
     * @param sender     The sender of the command, must be equal to the callback's player
     * @param uuidString The UUID obtained from registerCallback
     */
    public void handleCallback(T sender, String uuidString) {
        final UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return;
        }

        Callback callback = callbacks.get(uuid);
        if (callback == null) return;
        if (!Objects.equals(sender, callback.getSender())) return;

        // If it has expired and the scheduled task has already flipped the flag (or removed it),
        // do nothing. (If the removal hasn't happened yet but expired is set, still ignore.)
        if (callback.isExpired()) return;

        if (callback.isOneTime()) {
            // Ensure the "execute" happens only once for one-time callbacks.
            boolean firstRun = callback.markExecuted();
            callbacks.remove(uuid);
            if (!firstRun) return;
        }
        safeRun(callback.getUuid(), callback.getOnExecute());
    }

    private void safeRun(UUID uuid, Runnable r) {
        try {
            r.run();
        } catch (Exception ex) {
            logger.print("An unhandled exception occurred executing callback " + uuid, ex);
        }
    }

    @Getter
    @RequiredArgsConstructor
    private final class Callback {
        private final UUID uuid;
        private final long timeCreated;
        private final long expirationTime; // 0 means no expiration
        private final T sender;
        private final Runnable onExecute;
        private final boolean oneTime;

        private final AtomicBoolean executed = new AtomicBoolean(false);
        private final AtomicBoolean expired = new AtomicBoolean(false);

        boolean markExecuted() {
            return executed.compareAndSet(false, true);
        }

        boolean markExpired() {
            return expired.compareAndSet(false, true);
        }

        boolean isExpired() {
            return expired.get() || (expirationTime > 0 && System.currentTimeMillis() >= expirationTime);
        }
    }
}
