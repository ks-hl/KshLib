package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static dev.kshl.kshlib.misc.StringUtil.containsAnyOf;

public class StringUtilTest {
    @Test
    public void testContainsAnyOf() {
        assert containsAnyOf("ABC", "CDE");
        assert !containsAnyOf("ABC", "DEF");
    }
}
