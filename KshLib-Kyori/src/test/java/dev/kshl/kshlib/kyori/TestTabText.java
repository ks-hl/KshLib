package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.kshl.kshlib.kyori.TabText.width;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTabText {
    @Test
    public void testPadding() {
        for (int width = 0; width < 10; width++) {
            assertEquals(width, width(TabText.pad(width)));
        }
    }

    @Test
    public void testHeader() {
        Component left = Component.text("left");
        Component middle = Component.text("middle");
        Component right = Component.text("right component");

        assertEquals(TabText.CHAT_WINDOW_WIDTH, width(TabText.header(TabText.CHAT_WINDOW_WIDTH, List.of(left, middle, right))));

        // Test overflowing component is just built with no spaces
        assertEquals(width(left) + width(middle) + width(right), width(TabText.header(0, List.of(left, middle, right))));


        // TODO make sure middle is actually in the middle?
    }

    @Test
    public void testAlign() {
        assertEquals(". ", PlainTextComponentSerializer.plainText().serialize(TabText.parseSpacing(Component.text("."), 6, TabText.AlignDirection.LEFT)));
        assertEquals(" . ", PlainTextComponentSerializer.plainText().serialize(TabText.parseSpacing(Component.text("."), 10, TabText.AlignDirection.CENTER)));
        assertEquals(" .", PlainTextComponentSerializer.plainText().serialize(TabText.parseSpacing(Component.text("."), 6, TabText.AlignDirection.RIGHT)));
    }
}
