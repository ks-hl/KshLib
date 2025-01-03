package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

public class TestCaseInsensitiveMap {
    @Test
    public void testCaseInsensitiveMap() {
        CaseInsensitiveMap<Integer> map = new CaseInsensitiveMap<>();
        System.out.println("1. " + map);

        assert map.put("Test", 1) == null;
        System.out.println("2. " + map);

        assert map.put("tEst", 2) == 1;
        System.out.println("3. " + map);

        assert map.size() == 1;
        System.out.println("4. " + map);

        assert map.containsKey("TEST");
        System.out.println("5. " + map);

        assert map.computeIfAbsent("test", m -> 69) == 2;
        System.out.println("6. " + map);

        assert map.computeIfAbsent("test69", m -> 69) == 69;
        System.out.println("7. " + map);
    }
}
