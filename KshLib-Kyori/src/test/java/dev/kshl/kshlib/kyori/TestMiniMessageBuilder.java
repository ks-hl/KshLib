package dev.kshl.kshlib.kyori;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMiniMessageBuilder {
    private static final String COLOR = "<red>";
    private static final String HEX = "<#123456>";
    private static final String FORMAT = "<bold>";
    private static final String OBFUSCATE = "<obfuscated>";

    @Test
    public void testComponentHelper() {
        String full = ComponentHelper.replaceLegacyWithMini(COLOR + HEX + FORMAT + OBFUSCATE);
        assertEquals(full, new MiniMessageBuilder(full).build();
        assertEquals(FORMAT + OBFUSCATE, new MiniMessageBuilder(full).allowC, false, true, false, false, false, false, false));
        assertEquals(COLOR + FORMAT + OBFUSCATE, ComponentHelper.stripMiniMessage(full, true, false, false, false, false, false, false));
        assertEquals(COLOR + HEX + OBFUSCATE, ComponentHelper.stripMiniMessage(full, false, false, true, false, false, false, false));
        assertEquals(COLOR + HEX + FORMAT, ComponentHelper.stripMiniMessage(full, false, false, false, true, false, false, false));
    }

    @Test
    public void testLegacyMini() {
        assertEquals("<red><bold>hi </bold><red>hi", ComponentHelper.replaceLegacyWithMini("&c&lhi &chi"));
    }
}
