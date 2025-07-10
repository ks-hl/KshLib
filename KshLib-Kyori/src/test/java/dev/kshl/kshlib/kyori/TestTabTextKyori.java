package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.TabText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTabTextKyori {
    @Test
    public void testWidth() {
        test("hi");
        test("&e");
        test("&chi");
        test("&c&lhi");

        System.out.println(PlainTextComponentSerializer.plainText().serialize(TabTextKyori.header(
                Component.text("leftleftleft"),
                Component.text("center"),
                Component.text("right"),
                200)));
        System.out.println(PlainTextComponentSerializer.plainText().serialize(TabTextKyori.header(
                Component.text("left"),
                Component.text("center"),
                Component.text("right"),
                200)));
    }

    @Test
    public void testWrap() {
        assertEqualsComponents(Component.text("hi"), TabTextKyori.wrap("hi", Integer.MAX_VALUE));
        assertEqualsComponents(Component.text("hi hi hi"), TabTextKyori.wrap("hi hi hi", Integer.MAX_VALUE));
        assertEqualsComponents(Component.text(".. ..\n.."), TabTextKyori.wrap(".. .. ..", 12));
        assertEqualsComponents(Component.text(" ..\n ."), TabTextKyori.wrap(" .. .", 4));
        assertEqualsComponents(Component.text("..................."), TabTextKyori.wrap("...................", 4)); // Test too long of words just aren't wrapped
        assertEqualsComponents(Component.text(" - ...\n   .."), TabTextKyori.wrap(" - ... .", 10));
    }

    private static void assertEqualsComponents(Component expected, Component actual) {
        String expectedPlain = PlainTextComponentSerializer.plainText().serialize(expected);
        String actualPlain = PlainTextComponentSerializer.plainText().serialize(actual);
        assertEquals(expectedPlain, actualPlain);
    }

    private static void test(String str) {
        Component component = ComponentHelper.legacy(str);
        int stringWidth = TabText.width(str);
        int componentWidth = TabTextKyori.width(component);
        int componentWidth0 = width0(component);

        assertEquals(stringWidth, componentWidth);
        assertEquals(stringWidth, componentWidth0);
        assertEquals(componentWidth0, componentWidth);
    }

    static int width0(Component component) {
        String text = LegacyComponentSerializer.legacySection().serialize(component);
        return TabText.width(text);
    }
}
