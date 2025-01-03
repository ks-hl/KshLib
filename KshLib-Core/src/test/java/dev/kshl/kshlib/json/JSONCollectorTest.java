package dev.kshl.kshlib.json;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JSONCollectorTest {
    @Test
    public void testJSONCollector() {
        JSONArray arr = Arrays.stream(new Integer[]{1, 2, 3}).collect(new JSONCollector());
        assertEquals(3, arr.length());
        assertEquals("[1,2,3]", arr.toString());
    }
}
