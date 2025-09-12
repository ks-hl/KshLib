package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeSetStringTest {
    @Test
    public void testDuplicateDifferentCase() {
        TreeSetString treeSetString = new TreeSetString.CaseInsensitive();

        treeSetString.add("hi");
        treeSetString.add("HI");

        assertEquals(1, treeSetString.size());
    }
}
