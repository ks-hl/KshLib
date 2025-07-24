package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStringUtil {
    @Test
    public void testCount() {
        assertEquals(1, StringUtil.count(" ", ' '));
        assertEquals(2, StringUtil.count(" hello ", ' '));
        assertEquals(1, StringUtil.count("\uD83D\uDE42", "\uD83D\uDE42"));
    }
}
