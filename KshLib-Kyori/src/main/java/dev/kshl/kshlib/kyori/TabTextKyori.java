package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabTextKyori {
    public static Component header(@Nullable Component left, @Nullable Component center, @Nullable Component right, int width) {
        int leftWidth, centerWidth, rightWidth;
        if (left == null) {
            left = Component.text("");
            leftWidth = 0;
        } else {
            leftWidth = width(left);
        }
        if (center == null) {
            center = Component.text("");
            centerWidth = 0;
        } else {
            centerWidth = width(center);
        }
        if (right == null) {
            right = Component.text("");
            rightWidth = 0;
        } else {
            rightWidth = width(right);
        }

        int centerPos = width / 2 - centerWidth / 2;
        int leftPad = centerPos - leftWidth;

        int rightPos = width - rightWidth;
        int rightPad = rightPos - centerWidth - leftPad - leftWidth;

        TextComponent.Builder builder = Component.text();
        builder.append(left);
        builder.append(pad(leftPad));
        builder.append(center);
        builder.append(pad(rightPad));
        builder.append(right);
        return builder.build();
    }

    public static Component pad(int width) {
        TextColor textColor = TextColor.fromHexString("#161616");

        TextComponent.Builder builder = Component.text();

        int numSpaces = width / 4;
        width -= numSpaces * 4;
        String spaces = " ".repeat(numSpaces);
        if (!spaces.isEmpty()) {
            builder.append(Component.text(spaces));
        }

        int numDots = width / 2;
        width -= numDots * 2;
        String dots = ".".repeat(numDots);
        if (!dots.isEmpty()) {
            builder.append(Component.text(dots, textColor));
        }

        String one = Characters.ONE_LENGTH_DOT.repeat(width);
        if (!one.isEmpty()) {
            builder.append(Component.text(one, textColor));
        }

        return builder.build().compact();
    }

    public static int width(Component component) {
        if (component.children().isEmpty()) {
            if (component instanceof TextComponent textComponent) {
                return TabText.width(textComponent.content(), textComponent.hasDecoration(TextDecoration.BOLD));
            }
            return 0;
        } else {
            return component.children().stream().map(TabTextKyori::width).reduce(Integer::sum).orElse(0);
        }
    }

    public static Component wrap(String string, int wrapWidth) {
        if (wrapWidth <= 0) {
            throw new IllegalArgumentException("Invalid width, must be >0");
        }

        int leadingSpaceWidth = 0;
        int leadingSpaceIndex = 0;
        while (leadingSpaceIndex < string.length()) {
            char c = string.charAt(leadingSpaceIndex);
            if (c == '-') {
                leadingSpaceWidth += 6;
            } else if (c == ' ') {
                leadingSpaceWidth += 4;
            } else {
                break;
            }
            leadingSpaceIndex++;
        }
        Component leadingSpace = pad(leadingSpaceWidth);

        Component out = null;
        Component line = null;
        int lineWidth = 0;

        Pattern pattern = Pattern.compile("(\\s*)(\\S*)");
        Matcher matcher = pattern.matcher(string.substring(leadingSpaceIndex));
        boolean firstPart = true;

        while (matcher.find()) {
            String space = matcher.group(1);
            String part = matcher.group(2);

            if (firstPart) {
                space = string.substring(0, leadingSpaceIndex);
            }

            if (part.isEmpty()) continue;

            Component spaceComponent = Component.text(space);
            Component partComponent = ComponentHelper.legacy(part);

            int spaceWidth = width(spaceComponent);
            int partWidth = width(partComponent);

            if (lineWidth > 0 && lineWidth + spaceWidth + partWidth > wrapWidth) {
                out = appendLineTo(out, line, leadingSpace);
                line = null;
                lineWidth = 0;
                spaceComponent = Component.empty();
                spaceWidth = 0;
            }

            if (line == null) {
                line = spaceComponent.append(partComponent);
                lineWidth = spaceWidth + partWidth;
            } else {
                line = line.append(spaceComponent).append(partComponent);
                lineWidth += spaceWidth + partWidth;
            }

            firstPart = false;
        }

        if (line != null) {
            out = appendLineTo(out, line, leadingSpace);
        }

        return (out == null) ? Component.empty() : out.compact();
    }

    private static Component appendLineTo(Component out, Component line, Component leadingSpace) {
        if (out == null) {
            return line;
        } else {
            return out.appendNewline().append(leadingSpace).append(line);
        }
    }
}
