package dev.kshl.kshlib.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetUtilTest {
    @Test
    public void testTimeZoneRequest() throws IOException {
        TimeZoneAPI timeZoneAPI = new TimeZoneAPI();
        assert timeZoneAPI.getTimeZoneForIP("a849gfah") == null;
        assertEquals(TimeZone.getTimeZone("America/Los_Angeles"), timeZoneAPI.getTimeZoneForIP("64.233.185.138"));
    }
}
