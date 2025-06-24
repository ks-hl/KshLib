package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMiniMessageBuilder {
    private static final String COLOR = "<red>";
    private static final String HEX_COLOR = "#123456";
    private static final String HEX_TAG = "<" + HEX_COLOR + ">";
    private static final String FORMAT = "<bold>";
    private static final String OBFUSCATE = "<obfuscated>";
    private static final String SHADOW_COLOR = "#123456ff";
    private static final String SHADOW_TAG = "<shadow:" + SHADOW_COLOR + ">";
    private static final String MESSAGE = "Hello world";

    @Test
    public void testComponentHelper() {

        String full = COLOR + HEX_TAG + FORMAT + OBFUSCATE + SHADOW_TAG + MESSAGE;
        assertEquals(Component.text(full), new MiniMessageBuilder(full).build());
        assertEquals(Component.text(MESSAGE), new MiniMessageBuilder(full).stripDisallowed().build());
        assertEquals(Component.text(MESSAGE, NamedTextColor.RED), new MiniMessageBuilder(full).allowColors().stripDisallowed().build());
        assertEquals(Component.text(MESSAGE, TextColor.fromHexString(HEX_COLOR)), new MiniMessageBuilder(full).allowHex().stripDisallowed().build());
        assertEquals(Component.text(MESSAGE, null, TextDecoration.BOLD), new MiniMessageBuilder(full).allowBasicFormatting().stripDisallowed().build());
        assertEquals(Component.text(MESSAGE, null, TextDecoration.OBFUSCATED), new MiniMessageBuilder(full).allowObfuscation().stripDisallowed().build());
        assertEquals(Component.text(MESSAGE).shadowColor(ShadowColor.fromHexString(SHADOW_COLOR)), new MiniMessageBuilder(full).allowShadow().stripDisallowed().build());
    }

    @Test
    public void testLegacyMini() {
        assertEquals("<red><bold>hi </bold><red>hi", ComponentHelper.replaceLegacyWithMini("&c&lhi &chi"));
    }
}
