package dev.kshl.kshlib.bungee;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * TabText: class to write column formatted text in minecraft chat area
 *
 * - it splits each field and trims or fill with spaces to adjust tabs
 *
 * general usage example:
 *
 * - create a multiline string similar to csv format and create a TabText object with it
 * - use line feed "\n" (ascii 10) as line separator and grave accent "`" (ascii 96) as field separator
 * - you can use some format codes, see <a href="http://minecraft.gamepedia.com/Formatting_codes">http://minecraft.gamepedia.com/Formatting_codes</a>
 * - DO NOT USE LOWERCASE CODES OR BOLD FORMAT BECAUSE IT CAN BREAK SPACING
 *
 * - // example
 * - multilineString  = "PLAYER------`RATE------`RANK------\n";
 * - multilineString += "EJohn`10.01`1R\n";
 * - multilineString += "Doe`-9.30`2";
 *
 * - TabText tt = new TabText(multilineString);
 * - int numPages = tt.setPageHeight(pageHeight); // set page height and get number of pages
 * - tt.setTabs(10, 18, ...); // horizontal tabs positions
 * - tt.sortByFields(-2, 1); // sort by second column descending, then by first
 * - printedText = tt.getPage(desiredPage, (boolean) monospace); // get your formatted page, for console or chat area
 *
 * see each method javadoc for additional details
 *
 * &#64;version 5
 * &#64;author atesin#gmail,com
 * </pre>
 */

@Deprecated
public class MCTextAligner {

    public static int CHAT_WINDOW_WIDTH = 320;

    public static int width(String str) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '§') {
                if (++i == str.length()) {
                    break;
                }
                char next = str.charAt(i);
                if (next == 'l') {
                    bold = true;
                } else if (next >= 'a' && next <= 'f' || next == 'r' || next >= '0' && next <= '9') {
                    bold = false;
                }
                continue;
            } else if (c == '&' && i < str.length() - 7) {
                if (str.charAt(i + 1) == '#') {
                    i += 7;
                    bold = false;
                    continue;
                }
            }
            int thisWidth = switch (c) {
                case '!', '|', 'i', ';', ':', '.', ',' -> 2;
                case '\'', 'l', '`' -> 3;
                case ' ', 't', ']', '[', 'I', '*' -> 4;
                case '"', '}', '{', 'k', 'f', '>', '<', ')', '(' -> 5;
                default -> 6;
                case '@', '~' -> 7;
            };
            if (bold) thisWidth++;
            width += thisWidth;
        }
        return width;
    }

    public static String parseSpacing(String line, int size, AlignDirection align) {
        line = ChatColor.translateAlternateColorCodes('&', line);
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

    public static String header(int size, String motd) {
        motd = ChatColor.translateAlternateColorCodes('&', motd);
        String[] alignParts = motd.split(" *<a> *");
        if (alignParts.length > 1) {
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
        return motd;
    }

    public static String pad(int width) {
        StringBuilder pad = new StringBuilder(ChatColor.RESET + ChatColor.of("#111111").toString());
        while (width >= 2) {
            if (width >= 4) {
                pad.append(' ');
                width -= 4;
            } else {
                pad.append('.');
                width -= 2;
            }
        }
        return pad.toString();
    }

    private final String[] lines;
    private int[] tabs;

    public MCTextAligner(String multilineString) {
        lines = multilineString.split("\n", -1);
    }

    /**
     * set horizontal positions of "`" separators, considering 6px chars and 53
     * chars max
     *
     * @param tabs an integer list with desired tab column positions
     */
    public MCTextAligner setTabs(int... tabs) {
        int[] tabs2 = new int[tabs.length + 1];
        tabs2[0] = tabs[0];
        for (int i = 1; i < tabs.length; ++i)
            tabs2[i] = tabs[i] - tabs[i - 1];
        tabs2[tabs.length] = 53 - tabs[tabs.length - 1];
        this.tabs = tabs2;
        return this;
    }

    public MCTextAligner autoSizeTabs(int pad) {
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
     * get your formatted page, for chat area or console
     *
     * @return desired formatted, tabbed page
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

    @Override
    public String toString() {
        return build();
    }

    public enum AlignDirection {
        LEFT, CENTER, RIGHT
    }
}
