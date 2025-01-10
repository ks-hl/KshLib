package dev.kshl.kshlib.net.ip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestIPUtil {
    @Test
    public void testIntEncoding() {
        testIntEncoding("0.0.0.0");
        testIntEncoding("255.255.255.255");
        testIntEncoding("128.0.0.1");
        testIntEncoding("10.0.0.1");
        testIntEncoding("192.168.1.1");
        testIntEncoding("1.2.3.4");
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4(""));
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4("1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4("1.1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4("-1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4("1.1.1.-1"));
        assertThrows(IllegalArgumentException.class, () -> IPUtil.encodeV4("1.1.1.a"));
    }

    private static void testIntEncoding(String ip) {
        assertEquals(ip, IPUtil.decodeV4(IPUtil.encodeV4(ip)));
        assertEquals(ip, new IPv4(ip).toString());
    }

    @Test
    public void testSubnetMask() {
        assertEquals(0xFF_FF_FF_FFL, IPUtil.getSubnetMask(32));
        assertEquals(0xFF_FF_FF_00L, IPUtil.getSubnetMask(24));
        assertEquals(0xFF_FF_00_00L, IPUtil.getSubnetMask(16));
        assertEquals(0xFF_00_00_00L, IPUtil.getSubnetMask(8));
        assertEquals(0, IPUtil.getSubnetMask(0));
    }

    @Test
    public void testRangeStart() {
        assertEquals("0.0.0.0", IPUtil.getRangeStart("0.0.0.1", 0).toString());
        assertEquals("0.0.0.0", IPUtil.getRangeStart("255.255.255.255", 0).toString());
        assertEquals("0.0.0.0", IPUtil.getRangeStart("127.255.255.255", 1).toString());
        assertEquals("128.0.0.0", IPUtil.getRangeStart("128.0.0.1", 1).toString());
    }

    @Test
    public void testRangeEnd() {
        assertEquals("255.255.255.255", IPUtil.getRangeEnd("0.0.0.1", 0).toString());
        assertEquals("255.255.255.255", IPUtil.getRangeEnd("255.255.255.255", 0).toString());
        assertEquals("127.255.255.255", IPUtil.getRangeEnd("127.255.255.255", 1).toString());
        assertEquals("255.255.255.255", IPUtil.getRangeEnd("128.0.0.1", 1).toString());
        assertEquals("127.255.255.255", IPUtil.getRangeEnd("127.0.0.1", 1).toString());
    }
}
