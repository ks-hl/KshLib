package dev.kshl.kshlib.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BasicProfiler {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Thread watch;
    private final long life;
    private final Consumer<BasicProfiler> onStop;
    private final Map<StackTrace, AtomicLong> counter = new HashMap<>();
    private boolean running;
    private final Timer timer = new Timer("Profiler " + super.toString());
    private final AtomicInteger tickCount = new AtomicInteger();

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
        this.life = lifeMillis;
        this.onStop = onStop;
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Profiler already running");
        timer.reset();
        tickCount.set(0);
        running = true;
        new Thread(() -> {
            long lastTick = System.nanoTime();
            while (running) {
                long thisTick = System.nanoTime();
                tickCount.incrementAndGet();
                synchronized (counter) {
                    counter.computeIfAbsent(new StackTrace(watch.getStackTrace()), s -> new AtomicLong()).addAndGet(thisTick - lastTick);
                    lastTick = thisTick;
                    if (running && timer.getMillis() > life) {
                        stop();
                    }
                }
            }
        }).start();
    }

    public synchronized void stop() {
        if (!running) throw new IllegalStateException("Profiler not running");
        running = false;
        timer.pause();
        if (onStop != null) onStop.accept(this);
    }

    public synchronized Map<StackTrace, AtomicLong> getCountMap() {
        synchronized (counter) {
            return new HashMap<>(counter);
        }
    }

    public synchronized int getTickCount() {
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

    /**
     * Compiles the result of the profiling
     *
     * @param minimumThreshold A number between 0 and 1 which the portion of time a given entry used must exceed in order to be printed. -1 shows all entries, 1.01 would show none.
     * @return A compiled string explaining the results of the profiling
     */
    public String toString(double minimumThreshold) {
        double count = getTickCount();
        StringBuilder out = new StringBuilder("Profiler results: \nRan for: " + getRuntimeMillis() + "ms, sampled " + (int) count + " times");
        double totalRuntime = getRuntimeMillis();
        List<Map.Entry<StackTrace, AtomicLong>> map = new ArrayList<>(getCountMap().entrySet());
        map.sort(Comparator.comparingLong(e -> e.getValue().get()));
        for (Map.Entry<StackTrace, AtomicLong> entry : map) {
            long stackRuntime = entry.getValue().get();
            double factor = stackRuntime / totalRuntime;
            if (factor < minimumThreshold) continue;
            String toString = entry.getKey().toString(factor <= minimumThreshold * 0.01 ? 5 : 0);
            if (factor <= minimumThreshold * 0.01) toString = toString.trim();
            out.append(Math.round(stackRuntime / 1E5D) / 10d).append("ms, ").append(Math.round(factor * 1000) / 10d).append("%");
            out.append(toString);
            out.append("\n============\n");
        }
        return out.toString();
    }
}
