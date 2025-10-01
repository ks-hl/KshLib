package dev.kshl.kshlib.misc.snowflake;

public class SnowflakeOrdered implements Snowflake {
    public static final int COUNTER_FACTOR = 100_000;
    private long lastTime;
    private int counter;

    @Override
    public synchronized long getNextSnowflake() {
        long now = System.currentTimeMillis();
        if (now > lastTime) {
            lastTime = now;
            counter = 0;
        } else if (counter + 1 < COUNTER_FACTOR) {
            counter++;
        } else { // Resort to old method of skipping to the next millisecond. It's not pretty, but this should be an extreme edge case
            lastTime++;
            counter = 0;
        }
        return lastTime * COUNTER_FACTOR + counter;
    }
}
