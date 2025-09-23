package dev.kshl.kshlib.misc;

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

    /**
     * @param sender       The sender that must be equal when the callback is called
     * @param onExecute    What to do when the callback is executed
     * @param oneTime      Whether to restrict this callback to a single use
     * @param ttl          A TTL between 0 and 1 hour
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
        long time = System.currentTimeMillis();
        long expiration = time + ttl;
        callbacks.put(uuid, new Callback(uuid, time, expiration, sender, onExecute, oneTime));

        executor.schedule(() -> {
            Callback callback = callbacks.remove(uuid);
            if (!callback.checkFirstRun()) return;
            if (onExpiration != null) onExpiration.run();
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
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return;
        }
        Callback callback = callbacks.get(uuid);
        if (callback == null) return;
        if (!Objects.equals(sender, callback.getSender())) return;

        boolean firstRun = callback.checkFirstRun();
        if (callback.isOneTime()) {
            callbacks.remove(uuid);
            if (!firstRun) return;
        }

        callback.getOnExecute().run();
    }

    @Getter
    @RequiredArgsConstructor
    private final class Callback {
        private final UUID uuid;
        private final long timeCreated;
        private final long expirationTime;
        private final T sender;
        private final Runnable onExecute;
        private final boolean oneTime;
        private final AtomicBoolean executed = new AtomicBoolean();

        boolean checkFirstRun() {
            return getExecuted().compareAndSet(false, true);
        }
    }
}
