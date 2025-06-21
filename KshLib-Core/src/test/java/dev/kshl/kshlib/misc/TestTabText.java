package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTabText {
    @Test
    public void testTabText() {
        assertEquals(56, TabText.width("&cHello there"));
        assertEquals(67, TabText.width("&c&lHello there"));
        assertEquals(56, TabText.width("Hello there"));
    }
}
