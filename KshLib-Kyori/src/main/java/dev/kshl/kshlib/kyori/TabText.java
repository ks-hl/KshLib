package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabText {
    public static final int CHAT_WINDOW_WIDTH = 320;
    public static final int MULTIPLAYER_LIST_WIDTH = 265;

    public static final Map<String, Integer> widths = Collections.unmodifiableMap(new HashMap<>() {{
        for (String s : new String[]{Characters.BAR_1, Characters.ONE_LENGTH_DOT}) {
            put(s, 1);
        }
        for (String s : new String[]{"!", "|", "i", ";", ":", ".", ",", "'", Characters.BAR_2}) {
            put(s, 2);
        }
        for (String s : new String[]{"l", "`"}) {
            put(s, 3);
        }
        for (String s : new String[]{"\"", " ", "t", "]", "[", "I", "*", ")", "(", "}", "{", Characters.BAR_4}) {
            put(s, 4);
        }
        for (String s : new String[]{"k", "f", ">", "<", Characters.BAR_5}) {
            put(s, 5);
        }
        for (String s : new String[]{"@", "~"}) {
            put(s, 7);
        }
        put(Characters.BAR_9, 9);
    }});

    public static int width(Component component) {
        if (component == null) return 0;
        int width = 0;
        if (component instanceof TextComponent textComponent) {
            width += width(textComponent.content(), textComponent.hasDecoration(TextDecoration.BOLD));
        }
        for (Component child : component.children()) {
            width += width(child);
        }
        return width;
    }

    public static int width(String str, boolean bold) {
        if (str.codePoints().count() == 1) {
            String c = Character.toString(str.codePointAt(0));
            int width = widths.getOrDefault(c, 6);
            if (bold) width++;
            return width;
        }
        return str.codePoints().mapToObj(Character::toString).map(s -> width(s, bold)).reduce(Integer::sum).orElse(0);
    }

    public static Component parseSpacing(Component component, int size, AlignDirection align) {
        if (align == null) return component;
        int width = width(component);

        double diffWidth = size - width;
        if (align == AlignDirection.CENTER) diffWidth /= 2D;
        Component pad = pad((int) Math.floor(diffWidth));

        TextComponent.Builder out = Component.text();

        if (align == AlignDirection.RIGHT || align == AlignDirection.CENTER) {
            out.append(pad);
        }
        out.append(component);
        if (align == AlignDirection.LEFT || align == AlignDirection.CENTER) {
            out.append(pad);
        }

        return out.build().compact();
    }

    public static Component header(int size, List<Component> text) {
        if (text.isEmpty()) return Component.empty();
        if (text.size() == 1) return text.get(0);

        Component start = text.get(0);
        Component middle = text.get(1);
        Component end = text.size() > 2 ? text.get(2) : null;

        double startWidth = width(start);
        double midWidth = width(middle);
        double endWidth = width(end);

        TextComponent.Builder out = Component.text();
        out.append(start);
        if (midWidth > 0) {
            Component firstPad = pad((int) Math.round(size / 2D - startWidth - midWidth / 2D));
            out.append(firstPad);
            out.append(middle);
        }
        if (end != null) {
            int currentWidth = width(out.build());
            Component secondPad = pad((int) Math.round(size - currentWidth - endWidth));
            out.append(secondPad);
            out.append(end);
        }
        return out.build().compact();
    }

    public static Component pad(int width) {
        if (width <= 0) return Component.empty();
        TextComponent.Builder pad = Component.text();
        TextColor gray = TextColor.fromHexString("#161616");
        ShadowColor shadowColor = ShadowColor.shadowColor(0);
        while (width >= 1) {
            if (width >= 4) {
                pad.append(Component.text(' '));
                width -= 4;
            } else if (width >= 2) {
                pad.append(Component.text('.', gray).shadowColor(shadowColor));
                width -= 2;
            } else {
                pad.append(Component.text(Characters.ONE_LENGTH_DOT, gray).shadowColor(shadowColor));
                width -= 1;
            }
        }
        return pad.build().compact();
    }

    private final List<List<Component>> lines;
    private int[] tabs;
    private List<AlignDirection> alignDirections = new ArrayList<>();
    private AlignDirection defaultAlignDirection = AlignDirection.LEFT;

    public TabText(List<List<Component>> lines) {
        this.lines = lines;
    }

    /**
     * set horizontal positions of "`" separators, considering 6px chars and 53
     * chars max
     *
     * @param tabs an integer list with desired tab column positions
     */
    public TabText setTabs(int... tabs) {
        int[] tabs2 = new int[tabs.length + 1];
        tabs2[0] = tabs[0];
        for (int i = 1; i < tabs.length; ++i)
            tabs2[i] = tabs[i] - tabs[i - 1];
        tabs2[tabs.length] = 53 - tabs[tabs.length - 1];
        this.tabs = tabs2;
        return this;
    }

    /**
     * Automatically sizes the columns based on their content
     *
     * @param pad The number of pixels to add to the width of each column to pad it
     */
    public TabText autoSizeTabs(int pad) {
        List<Integer> tabs = new ArrayList<>();
        for (List<Component> line : lines) {
            for (int fieldPos = 0; fieldPos < line.size(); fieldPos++) {
                int width = fieldPos < tabs.size() ? tabs.get(fieldPos) : 0;
                int fieldWidth = width(line.get(fieldPos)) + pad;
                if (fieldWidth > width) {
                    width = fieldWidth;
                    while (fieldPos >= tabs.size()) {
                        tabs.add(0);
                    }
                    tabs.set(fieldPos, width);
                }
            }
        }
        this.tabs = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            this.tabs[i] = tabs.get(i);
        }
        return this;
    }

    public TabText align(List<AlignDirection> alignDirections) {
        this.alignDirections = alignDirections;
        return this;
    }

    public TabText defaultAlign(AlignDirection alignDirection) {
        this.defaultAlignDirection = alignDirection;
        return this;
    }

    /**
     * @return formatted, tabbed page
     */
    public Component build() {
        TextComponent.Builder outputBuilder = Component.text();
        boolean outputBuilderIsEmpty = true;
        for (List<Component> line : lines) {
            TextComponent.Builder lineBuilder = Component.text();
            for (int fieldPos = 0; fieldPos < line.size(); ++fieldPos) {
                int tab = 0;
                if (fieldPos < tabs.length) tab = tabs[fieldPos];
                AlignDirection alignDirection = defaultAlignDirection;
                if (fieldPos < alignDirections.size()) alignDirection = alignDirections.get(fieldPos);
                lineBuilder.append(parseSpacing(line.get(fieldPos), tab, alignDirection));
            }
            if (outputBuilderIsEmpty) {
                outputBuilderIsEmpty = false;
            } else {
                outputBuilder.appendNewline();
            }
            outputBuilder.append(lineBuilder);
        }
        return outputBuilder.build();
    }

    public enum AlignDirection {
        LEFT, CENTER, RIGHT
    }
}
