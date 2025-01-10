package dev.kshl.kshlib.net.ip;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCIDRRange {
    @Test
    public void testRanges() {
        testRange("0.0.0.0", 30, "0.0.0.0", "0.0.0.1", "0.0.0.2", "0.0.0.3");
        testRange("0.0.0.4", 30, "0.0.0.4", "0.0.0.5", "0.0.0.6", "0.0.0.7");
        testRange("0.0.0.7", 30, "0.0.0.4", "0.0.0.5", "0.0.0.6", "0.0.0.7");
        testRange("0.0.0.0", 31, "0.0.0.0", "0.0.0.1");
        testRange("0.0.0.1", 31, "0.0.0.0", "0.0.0.1");
        testRange("0.0.0.0", 32, "0.0.0.0");
    }

    @Test
    public void testSize() {
        assertEquals(1, new CIDRRange("0.0.0.0", 32).size());
        assertEquals(2, new CIDRRange("0.0.0.0", 31).size());
        assertEquals(4, new CIDRRange("0.0.0.0", 30).size());
        assertEquals(0x1_00_00_00_00L, new CIDRRange("0.0.0.0", 0).size());
        assertEquals(0x1_00_00_00L, new CIDRRange("0.0.0.0", 8).size());
        assertEquals(0x1_00_00L, new CIDRRange("0.0.0.0", 16).size());
        assertEquals(0x1_00L, new CIDRRange("0.0.0.0", 24).size());
    }

    private static void testRange(String base, int cidr, String... ips) {
        Set<String> range = new HashSet<>(List.of(ips));

        for (IPv4 iPv4 : new CIDRRange(base, cidr)) {
            assertTrue(range.remove(iPv4.toString()), () -> "Set did not contain " + iPv4);
        }

        assertTrue(range.isEmpty());
    }
}
