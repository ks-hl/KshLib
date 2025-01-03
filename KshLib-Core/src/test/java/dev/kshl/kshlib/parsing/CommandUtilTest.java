package dev.kshl.kshlib.parsing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandUtilTest {
    @Test
    public void testKeyValueParsing() {
        var result = CommandUtil.parseKeyValue("fgbs * afze:fe-_':b bfez:cfze :df eFE: e e:fefwf #testflag testjunk");
        assertEquals("fe-_':b", result.keyValues().get("afze"));
        assertEquals("cfze :df eFE: e", result.keyValues().get("bfez"));
        assertEquals("fefwf", result.keyValues().get("e"));

        assert result.flags().contains("testflag");

        assert result.ignored().contains("fgbs");
        assert result.ignored().contains("*");
        assert result.ignored().contains("testjunk");
    }

    @Test
    public void testStartsWith() {
        List<String> options = List.of("abc", "zzz", "cba");
        assertEquals(List.of("abc"), CommandUtil.filterStartsWith(options, "a", false));
        assertEquals(List.of("abc"), CommandUtil.filterStartsWith(options, "A", false));
        assertEquals(List.of(), CommandUtil.filterStartsWith(options, "A", true));
    }

    @Test
    public void testContains() {
        List<String> options = List.of("abc", "zzz", "cba");
        assertEquals(List.of("abc", "cba"), CommandUtil.filterContains(options, "a", false));
        assertEquals(List.of("abc", "cba"), CommandUtil.filterContains(options, "A", false));
        assertEquals(List.of(), CommandUtil.filterContains(options, "A", true));
    }
}
