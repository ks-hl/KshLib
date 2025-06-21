package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void testBasicEquality() {
        assertEquals(0, new Version("1.2.3").compareTo(new Version("1.2.3")));
        assertEquals(0, new Version("v1.2.3").compareTo(new Version("1.2.3")));
    }

    @Test
    void testDifferentLengths() {
        assertEquals(0, new Version("1.2.0").compareTo(new Version("1.2")));
        assertTrue(new Version("1.2").compareTo(new Version("1.2.1")) < 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2")) > 0);
    }

    @Test
    void testVersionOrdering() {
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.4")) < 0);
        assertTrue(new Version("2.0.0").compareTo(new Version("1.9.9")) > 0);
        assertTrue(new Version("1.10.0").compareTo(new Version("1.2.0")) > 0);
    }

    @Test
    void testPreReleaseHandling() {
        assertTrue(new Version("1.2.3-pre1").compareTo(new Version("1.2.3")) < 0);
        assertTrue(new Version("1.2.3-rc1").compareTo(new Version("1.2.3")) < 0);
        assertTrue(new Version("1.2.3-pre1").compareTo(new Version("1.2.3-rc1")) < 0);
        assertTrue(new Version("1.2.3-pre2").compareTo(new Version("1.2.3-pre1")) > 0);
        assertTrue(new Version("1.2.3-rc2").compareTo(new Version("1.2.3-rc1")) > 0);
    }

    @Test
    void testMalformedStrings() {
        assertThrows(IllegalArgumentException.class, () -> new Version("abc"));
        assertThrows(IllegalArgumentException.class, () -> new Version("1.2..3"));
        assertThrows(IllegalArgumentException.class, () -> new Version("1.2.3-beta"));
        assertThrows(IllegalArgumentException.class, () -> new Version("v"));
        assertThrows(IllegalArgumentException.class, () -> new Version("v1..2"));
    }

    @Test
    void testLeadingVHandling() {
        assertEquals(new Version("v1.2.3"), new Version("1.2.3"));
        assertTrue(new Version("v1.0").compareTo(new Version("1.1")) < 0);
    }

    @Test
    void testEquals() {
        assertEquals(new Version("1.2.3"), new Version("1.2.3"));
    }

    @Test
    void testHashCode() {
        String v1 = "1.1";
        String v2 = "1.2";
        String v3 = "1.3";
        int v1Hash = new Version(v1).hashCode();
        int v2Hash = new Version(v2).hashCode();
        int v3Hash = new Version(v3).hashCode();

        assertNotEquals(v1Hash, v2Hash);
        assertNotEquals(v2Hash, v3Hash);
        assertNotEquals(v1Hash, v3Hash);

        assertEquals(v1Hash, new Version(v1).hashCode());
        assertEquals(v2Hash, new Version(v2).hashCode());
        assertEquals(v3Hash, new Version(v3).hashCode());
    }

    @Test
    void testSingleDigitVersions() {
        assertEquals(0, new Version("1").compareTo(new Version("1.0.0")));
        assertTrue(new Version("2").compareTo(new Version("1")) > 0);
        assertTrue(new Version("1").compareTo(new Version("2")) < 0);
    }

    @Test
    void testZeroPaddingEffect() {
        assertEquals(0, new Version("1.02.003").compareTo(new Version("1.2.3")));
    }

    @Test
    void testComplexComparisons() {
        assertTrue(new Version("1.2.3-pre1").compareTo(new Version("1.2.3-pre2")) < 0);
        assertTrue(new Version("1.2.3-rc1").compareTo(new Version("1.2.3-pre2")) > 0);
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.3-rc5")) > 0);
    }

    @Test
    void testNewerOlder() {
        assertTrue(new Version("1.1").isNewerThan(new Version("1.0")));
        assertTrue(new Version("1.1").isOlderThan(new Version("1.2")));
    }
}
