package dev.kshl.kshlib.yaml;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlTest {
    @Test
    public void testYaml() throws IOException {
        File file = new File("test/test.yml");
        file.delete();
        YamlConfig yamlConfig = new YamlConfig(file, null);
        yamlConfig.initializeDataMap();

        yamlConfig.set("hi", "hello!");
        yamlConfig.set("hib", true);
        yamlConfig.set("yes", "Yes");
        yamlConfig.set("no", "No");
        yamlConfig.set("section1.section2.bool", true);

        for (String s : new String[]{"a", "b", "c"}) {
            YamlConfig section = yamlConfig.getOrCreateSection("section." + s);
            section.set("val", s);
        }
        // TODO more

        final int preSaveHash = yamlConfig.hashCode();
        yamlConfig.save();
        yamlConfig = new YamlConfig(file, null).load();

        System.out.println("==============");
        System.out.println(yamlConfig);
        System.out.println("==============");

        assertEquals(preSaveHash, yamlConfig.hashCode());

        assertEquals("hello!", yamlConfig.getString("hi").orElseThrow());
        assertEquals("Yes", yamlConfig.getString("yes").orElseThrow());
        assertEquals("No", yamlConfig.getString("no").orElseThrow());
        assert yamlConfig.getBoolean("hib").orElse(false);
        assert yamlConfig.getSection("section1").isPresent();
        assert yamlConfig.getSection("section1").flatMap(s -> s.getSection("section2")).flatMap(s2 -> s2.getBoolean("bool")).orElseThrow();
        assert yamlConfig.getBoolean("section1.section2.bool").orElseThrow();
        assertEquals("a", yamlConfig.getString("section.a.val").orElseThrow());
        assertEquals("b", yamlConfig.getString("section.b.val").orElseThrow());
        assertEquals("c", yamlConfig.getString("section.c.val").orElseThrow());
        assertEquals(6, yamlConfig.getKeys(false).size());

        YamlConfig finalYamlConfig = yamlConfig;
        assertThrows(IllegalArgumentException.class, () -> finalYamlConfig.set(".df", null));
        assertThrows(IllegalArgumentException.class, () -> finalYamlConfig.set("df.", null));
        assertThrows(IllegalArgumentException.class, () -> finalYamlConfig.set("", null));
        assertThrows(IllegalArgumentException.class, () -> finalYamlConfig.set(null, null));
    }
}
