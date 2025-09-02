package dev.kshl.kshlib.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BasicProfiler {
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BasicProfiler-Sampler");
                t.setDaemon(true);
                return t;
            });

    private final Thread watch;
    private final long lifeMillis;
    private final Consumer<BasicProfiler> onStop;
    private final Map<StackTraceKey, AtomicLong> counter = new ConcurrentHashMap<>();
    private final AtomicInteger tickCount = new AtomicInteger();
    private final Timer timer = new Timer();
    private volatile boolean running;

    private ScheduledFuture<?> task;

    // Default sampling interval in ms
    private final long sampleIntervalMillis = 10;

    public BasicProfiler(long lifeMillis) {
        this(lifeMillis, null);
    }

    public BasicProfiler(long lifeMillis, Consumer<BasicProfiler> onStop) {
        this(Thread.currentThread(), lifeMillis, onStop);
    }

    public BasicProfiler(Thread watch, long lifeMillis) {
        this(watch, lifeMillis, null);
    }

    public BasicProfiler(Thread watch, long lifeMillis, Consumer<BasicProfiler> onStop) {
        this.watch = watch;
        this.lifeMillis = lifeMillis;
        this.onStop = onStop;
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Profiler already running");
        running = true;
        tickCount.set(0);
        counter.clear();
        timer.reset();

        task = executor.scheduleAtFixedRate(() -> {
            if (!running) return;
            tickCount.incrementAndGet();
            StackTraceKey key = new StackTraceKey(watch.getStackTrace());
            counter.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(sampleIntervalMillis);
            if (timer.getMillis() >= lifeMillis) {
                stop();
            }
        }, 0, sampleIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (!running) throw new IllegalStateException("Profiler not running");
        running = false;
        timer.pause();
        if (task != null) task.cancel(false);
        if (onStop != null) onStop.accept(this);
    }

    public Map<StackTraceKey, AtomicLong> getCountMap() {
        return new ConcurrentHashMap<>(counter);
    }

    public int getTickCount() {
        return tickCount.get();
    }

    public long getStartedTime() {
        return timer.getStartMillis();
    }

    public double getRuntimeMillis() {
        return timer.getMillis();
    }

    @Override
    public String toString() {
        return toString(-1);
    }

    public String toString(double minimumThreshold) {
        StringBuilder out = new StringBuilder();
        out.append("Profiler results:\n")
                .append("Ran for: ").append(getRuntimeMillis()).append(" ms, ")
                .append("sampled ").append(getTickCount()).append(" times\n");

        double totalTime = getRuntimeMillis();
        List<Map.Entry<StackTraceKey, AtomicLong>> entries = new ArrayList<>(getCountMap().entrySet());
        entries.sort(Comparator.comparingLong(e -> -e.getValue().get())); // descending

        for (Map.Entry<StackTraceKey, AtomicLong> entry : entries) {
            long timeMs = entry.getValue().get();
            double factor = timeMs / totalTime;
            if (minimumThreshold >= 0 && factor < minimumThreshold) continue;

            out.append(String.format("%.1fms, %.1f%%\n%s\n============\n",
                    (double) timeMs, factor * 100, entry.getKey()));
        }
        return out.toString();
    }

    public record StackTraceKey(StackTraceElement[] elements) {
    }
}
