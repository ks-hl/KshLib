package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimerTest {
    @Test
    public void testTimer() throws InterruptedException {
        Timer timer = new Timer("test");
        long start = System.currentTimeMillis();
        Thread.sleep(20);
        timer.pause();
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(elapsed, timer.getMillis(), 7);
        Thread.sleep(10);
        assertEquals(elapsed, timer.getMillis(), 7);
        timer.resume();
        start = System.currentTimeMillis();
        Thread.sleep(20);
        elapsed += System.currentTimeMillis() - start;
        assertEquals(elapsed, timer.getMillis(), 7);
        timer.reset();
        start = System.currentTimeMillis();
        Thread.sleep(10);
        assertEquals(System.currentTimeMillis() - start, timer.getMillis(), 7);
        System.out.println(timer);
    }
}
