package dev.kshl.kshlib.net.ip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestIPv4 {
    @Test
    public void testCompare() {
        assertEquals(-1, new IPv4("0.0.0.0").difference(new IPv4("0.0.0.1")));
        assertEquals(256, new IPv4("0.0.1.0").difference(new IPv4("0.0.0.0")));
        assertEquals(255, new IPv4("0.0.1.0").difference(new IPv4("0.0.0.1")));
        assertEquals(0, new IPv4("0.0.1.0").difference(new IPv4("0.0.1.0")));

        assertEquals(-1, new IPv4("0.0.0.0").compareTo(new IPv4("0.0.0.1")));
        assertEquals(1, new IPv4("0.0.1.0").compareTo(new IPv4("0.0.0.0")));
        assertEquals(1, new IPv4("0.0.1.0").compareTo(new IPv4("0.0.0.1")));
        assertEquals(0, new IPv4("0.0.1.0").compareTo(new IPv4("0.0.1.0")));
    }
}
