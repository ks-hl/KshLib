package dev.kshl.kshlib.misc;

public class Timer {
    private final String name;
    private final double oom;
    private long start;
    private long paused;
    private long pausedFor;

    public Timer() {
        this(null);
    }

    public Timer(String name) {
        this(name, 1);
    }

    public Timer(int oom) {
        this(null, oom);
    }

    public Timer(String name, int oom) {
        this.name = name;
        this.oom = Math.pow(10, oom);

        start = System.nanoTime();
    }

    public void resume() {
        if (paused == 0) return;
        pausedFor += System.nanoTime() - paused;
        paused = 0;
    }

    public void pause() {
        if (paused > 0) return;
        paused = System.nanoTime();
    }

    public void reset() {
        paused = pausedFor = 0;
        start = System.nanoTime();
    }

    public long getTotalRuntimeNanos() {
        if (start == 0) return 0;
        long end = paused > 0 ? paused : System.nanoTime();
        return end - start - pausedFor;
    }

    @Override
    public String toString() {
        return (name == null ? "" : (name + ": ")) + Math.round(getMillis() * oom) / oom + "ms";
    }

    public double getMillis() {
        return getTotalRuntimeNanos() / 1000000.0;
    }

    public long getStartNanos() {
        return start;
    }

    public long getStartMillis() {
        return getStartNanos() / 1000000;
    }

    public boolean isPaused() {
        return paused > 0;
    }
}
