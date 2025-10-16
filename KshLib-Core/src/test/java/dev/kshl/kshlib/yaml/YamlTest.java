package dev.kshl.kshlib.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlTest {
    private static final File file = new File("test/test.yml");
    YamlConfig yamlConfig;

    @BeforeEach
    public void init() {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        yamlConfig = new YamlConfig(file, null);
        yamlConfig.initializeDataMap();
    }

    private void reload() throws IOException {
        final int preSaveHash = yamlConfig.hashCode();
        yamlConfig.save();
        yamlConfig = new YamlConfig(file, null).load();

        System.out.println("==============");
        System.out.println(yamlConfig);
        System.out.println("==============");

        assertEquals(preSaveHash, yamlConfig.hashCode());
    }

    @Test
    public void testSections() throws IOException {
        yamlConfig.set("section1.section2.bool", true);

        for (String s : new String[]{"a", "b", "c"}) {
            YamlConfig section = yamlConfig.getOrCreateSection("section." + s);
            section.set("val", s);
        }

        reload();

        assert yamlConfig.getSection("section1").isPresent();
        assert yamlConfig.getSection("section1").flatMap(s -> s.getSection("section2")).flatMap(s2 -> s2.getBoolean("bool")).orElseThrow();
        assert yamlConfig.getBoolean("section1.section2.bool").orElseThrow();

        assertEquals("a", yamlConfig.getString("section.a.val").orElseThrow());
        assertEquals("b", yamlConfig.getString("section.b.val").orElseThrow());
        assertEquals("c", yamlConfig.getString("section.c.val").orElseThrow());
    }

    @Test
    public void testBasicValues() throws IOException {
        yamlConfig.set("hi", "hello!");
        yamlConfig.set("i", 1);
        yamlConfig.set("l", Long.MAX_VALUE);
        yamlConfig.set("hib", true);
        yamlConfig.set("yes", "Yes");
        yamlConfig.set("no", "No");

        reload();

        assertEquals("hello!", yamlConfig.getString("hi").orElseThrow());

        assertEquals(1, yamlConfig.getIntResult("i").orElseThrow());
        assertEquals(1, yamlConfig.getLongResult("i").orElseThrow());
        assertEquals(1, yamlConfig.getDoubleResult("i").orElseThrow());

        assertEquals(Long.MAX_VALUE, yamlConfig.getLongResult("l").orElseThrow());

        assert yamlConfig.getBoolean("hib").orElse(false);

        // There were issues with "yes"/"no" being converted to booleans
        assertEquals("Yes", yamlConfig.getString("yes").orElseThrow());
        assertEquals("No", yamlConfig.getString("no").orElseThrow());
        assert !yamlConfig.getKeys(false).isEmpty();
    }

    @Test
    public void testSectionLists() throws IOException {
        List<YamlConfig> sectionList = new ArrayList<>();
        YamlConfig one = YamlConfig.empty();
        sectionList.add(one);
        one.set("index", 1);
        one.set("other_thing", 1);
        YamlConfig two = YamlConfig.empty();
        sectionList.add(two);
        two.set("index", 2);
        two.set("other_thing", 1);
        yamlConfig.set("sectionlist", sectionList);

        for (String s : new String[]{"a", "b", "c"}) {
            YamlConfig section = yamlConfig.getOrCreateSection("section." + s);
            section.set("val", s);
        }

        reload();

        assertEquals("a", yamlConfig.getString("section.a.val").orElseThrow());
        assertEquals("b", yamlConfig.getString("section.b.val").orElseThrow());
        assertEquals("c", yamlConfig.getString("section.c.val").orElseThrow());
        assert !yamlConfig.getKeys(false).isEmpty();

        List<YamlConfig> section = yamlConfig.getSectionList("sectionlist").orElseThrow();


        assertEquals("sectionlist[0]", section.get(0).getFullKey());

        assertEquals(2, section.size());

    }

    @Test
    public void testInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> yamlConfig.set(".df", null));
        assertThrows(IllegalArgumentException.class, () -> yamlConfig.set("df.", null));
        assertThrows(IllegalArgumentException.class, () -> yamlConfig.set("", null));
        assertThrows(IllegalArgumentException.class, () -> yamlConfig.set(null, null));
    }

    @Test
    public void testEnum() throws IOException {
        yamlConfig.set("enum", FakeEnum.ENUM1);
        yamlConfig.set("enum2", FakeEnum.ENUM2.toString().toLowerCase());
        reload();
        assertEquals("ENUM1", yamlConfig.getString("enum").orElseThrow());
        assertEquals(FakeEnum.ENUM1, yamlConfig.getEnumResult("enum", FakeEnum.class).orElseThrow());
        assertEquals(FakeEnum.ENUM2, yamlConfig.getEnumResult("enum2", FakeEnum.class).orElseThrow());
    }

    @Test
    public void testResultErrorMessages() {
        var result1 = yamlConfig.getIntResult("irgjiue");
        try {
            result1.orElseThrow();
            assert false;
        } catch (IllegalArgumentException e) {
            assertEquals("Missing 'irgjiue'", e.getMessage());
        }

        yamlConfig.set("hi.hi", "hi");
        var result2 = yamlConfig.getIntResult("hi.hi");
        try {
            result2.orElseThrow();
            assert false;
        } catch (IllegalArgumentException e) {
            assertEquals("'hi.hi' is wrong type. Expected 'Integer', is 'java.lang.String'", e.getMessage());
        }
    }
}
