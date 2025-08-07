package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TabTextAligner {
    @Test
    public void align() {
        String text = """
                                - "text:&f\\\\&0&0 Black`&f\\\\&1&1 Dark Blue`&f\\\\&2&2 Dark Green`&f\\\\&3&3 Teal"
                                - "text:&f\\\\&4&4 Dark Red`&f\\\\&5&5 Purple`&f\\\\&6&6 Gold`&f\\\\&7&7 Gray"
                                - "text:&f\\\\&8&8 Dark Gray`&f\\\\&9&9 Indigo`&f\\\\&a&a Bright Green`&f\\\\&b&b Cyan"
                                - "text:&f\\\\&c&c Red`&f\\\\&d&d Pink`&f\\\\&e&e Yellow`&f\\\\&f&f White"
                                - "text:\\\\&l <bold>Bold</bold>`\\\\&m <st>Strike</st>`\\\\&n <u>Underline</u>`\\\\&o <i>Italic</i>"
                                - "text:\\\\&k&k Magic&r`\\\\&r Reset"
                """;
        String removeFirst = "- \"text:";
        String[] lines = text.split("[\n\r]+");
        List<List<Component>> parts = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(removeFirst)) line = line.substring(removeFirst.length());
            if (line.endsWith("\"")) line = line.substring(0, line.length() - 1);
            List<Component> lineParts = new ArrayList<>();
            for (String part : line.split("`")) {
                lineParts.add(new MiniMessageBuilder(part).allowAll().build());
            }
            parts.add(lineParts);
        }
        Component component = new dev.kshl.kshlib.kyori.TabText(parts).autoSizeTabs(6).build();
        String linebreak = "$$LINEBREAK$$";
        component = component.replaceText(builder -> builder.match("[\n\r]+").replacement(linebreak));
        System.out.println(removeFirst + MiniMessage.miniMessage().serialize(component).replace(linebreak, "\"\n" + removeFirst) + "\"");
    }
}
