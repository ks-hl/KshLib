package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.TabText;
import net.kyori.adventure.text.Component;
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

    private static void test(String str) {
        Component component = ComponentHelper.legacy(str);
        int stringWidth = TabText.width(str);
        int componentWidth = TabTextKyori.width(component);
        int componentWidth0 = TabTextKyori.width0(component);

        assertEquals(stringWidth, componentWidth);
        assertEquals(stringWidth, componentWidth0);
        assertEquals(componentWidth0, componentWidth);
    }
}
