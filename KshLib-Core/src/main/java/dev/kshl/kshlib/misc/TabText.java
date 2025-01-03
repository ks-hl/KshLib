package dev.kshl.kshlib.misc;

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

    public static int width(String str) {
        str = str.replace("§", "&");
        int width = 0;
        boolean bold = false;
        List<String> chars = str.codePoints().mapToObj(Character::toString).toList();
        for (int i = 0; i < chars.size(); i++) {
            String c = chars.get(i);
            if (c.equals("&")) {
                if (++i == chars.size()) {
                    break;
                }
                String next = chars.get(i);
                if (next.length() > 1) continue;
                char nextChar = next.charAt(0);
                if (nextChar == 'l') {
                    bold = true;
                } else if (nextChar >= 'a' && nextChar <= 'f' || nextChar == 'r' || nextChar >= '0' && nextChar <= '9') {
                    bold = false;
                } else if (i < str.length() - 6) {
                    if (nextChar == '#') {
                        i += 6;
                        bold = false;
                        continue;
                    }
                }
                continue;
            }
            int thisWidth = widths.getOrDefault(c, 6);
            if (bold) thisWidth++;
            width += thisWidth;
        }
        return width;
    }

    public static String parseSpacing(String line, int size, AlignDirection align) {
        int width = width(line);

        double toAdd = size - width;
        if (align == AlignDirection.CENTER) toAdd /= 2;
        String pad = pad((int) Math.round(toAdd));

        if (align == AlignDirection.RIGHT || align == AlignDirection.CENTER) line = pad + line;
        if (align == AlignDirection.LEFT || align == AlignDirection.CENTER) line += pad;
        return line;
    }

    public static List<String> split(String string, int size) {
        List<String> out = new ArrayList<>();

        StringBuilder word = new StringBuilder();
        StringBuilder line = new StringBuilder();

        char[] cArray = string.trim().toCharArray();
        for (int i = 0; i < cArray.length; i++) {
            char c = cArray[i];
            word.append(c);
            if (c == ' ' || i == cArray.length - 1) {
                int wordWidth = width(word.toString());
                if ((wordWidth + width(line.toString()) > size || wordWidth >= size) && !line.isEmpty()) {
                    out.add(line.toString());
                    line = new StringBuilder();
                }
                line.append(word);
                word = new StringBuilder();
            }
        }
        if (!line.isEmpty()) out.add(line.toString());

        return out;
    }

    public static String header(int size, String text) {
        String[] alignParts = text.split("\\s*<a>\\s*");
        if (alignParts.length <= 1) return text;

        String start = alignParts[0];
        String middle = alignParts[1];
        String end = "";
        if (alignParts.length > 2) {
            end = alignParts[2];
        }
        int startWidth = width(start);
        int midWidth = width(middle);
        int endWidth = width(end);

        String pad = pad((int) Math.round((double) (size - startWidth - midWidth - endWidth) / (2D)));
        return start + pad + middle + pad + end;
    }

    public static String pad(int width) {
        StringBuilder pad = new StringBuilder("&#161616");
        while (width >= 1) {
            if (width >= 4) {
                pad.append(' ');
                width -= 4;
            } else if (width >= 2) {
                pad.append('.');
                width -= 2;
            } else {
                pad.append(Characters.ONE_LENGTH_DOT);
                width -= 1;
            }
        }
        return pad.toString();
    }

    private String[] lines;
    private int[] tabs;

    public TabText(String multilineString) {
        lines = multilineString.split("\n", -1);
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
        for (String line : lines) {
            String[] fields = line.split("`");
            for (int fieldPos = 0; fieldPos < fields.length; fieldPos++) {
                int width = fieldPos < tabs.size() ? tabs.get(fieldPos) : 0;
                int fieldWidth = width(fields[fieldPos]) + pad;
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

    /**
     * @return formatted, tabbed page
     */
    public String build() {
        StringBuilder outputBuilder = new StringBuilder();
        String[] fields;
        for (String s : lines) {

            fields = s.split("`");
            StringBuilder lineBuilder = new StringBuilder();

            int lineLengthActual = 0, lineLengthDesired = 0;

            for (int fieldPos = 0; fieldPos < fields.length; ++fieldPos) {
                int tab = tabs[fieldPos];
                lineLengthDesired += tab;
                lineLengthActual += width(fields[fieldPos]);
                int needPad = lineLengthDesired - lineLengthActual;
                String pad = pad(needPad);
                lineLengthActual += width(pad);

                lineBuilder.append("§f").append(fields[fieldPos]);
                lineBuilder.append(pad);
            }
            outputBuilder.append(outputBuilder.isEmpty() ? lineBuilder.toString() : '\n' + lineBuilder.toString());
        }
        return outputBuilder.toString();
    }

    public enum AlignDirection {
        LEFT, CENTER, RIGHT
    }
}
