package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.TabText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertEqualsComponents("hi", TabTextKyori.wrap("hi", Integer.MAX_VALUE));
        assertEqualsComponents("", TabTextKyori.wrap("&f", Integer.MAX_VALUE));
        assertEqualsComponents("hi hi hi", TabTextKyori.wrap("hi hi hi", Integer.MAX_VALUE));
        assertEqualsComponents("&0hi hi hi", TabTextKyori.wrap("&0hi hi hi", Integer.MAX_VALUE));
        assertEqualsComponents("&0hi &ahi&bhi&chi", TabTextKyori.wrap("&0hi &ahi&bhi&chi", Integer.MAX_VALUE));
        assertEqualsComponents("&0hi &ahi &bhi", TabTextKyori.wrap("&0hi &ahi&b hi", Integer.MAX_VALUE));
        assertEqualsComponents("&0..\n&0..", TabTextKyori.wrap("&0.. ..", 4)); // Test color inheritance
        assertEqualsComponents(".. ..\n..", TabTextKyori.wrap(".. .. ..", 12));
        assertEqualsComponents(" ..\n .", TabTextKyori.wrap(" .. .", 4));
        assertEqualsComponents("...................", TabTextKyori.wrap("...................", 4)); // Test too long of words just aren't wrapped
        assertEqualsComponents(" - ...\n   &0.&r.", TabTextKyori.wrap(" - ... .", 10));
    }

    private static void assertEqualsComponents(String expected, List<Component> actual) {
        String expectedPlain = expected.replace("\n", "\\n");
        String actualPlain = actual.stream().map(LegacyComponentSerializer.legacyAmpersand()::serialize).reduce((a, b) -> a + "\\n" + b).orElse("");
        System.out.println(expectedPlain + "==" + actualPlain);
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
