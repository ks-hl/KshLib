package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;
import java.util.Arrays;
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

        String spaces = " ".repeat(width / 4);
        if (!spaces.isEmpty()) {
            builder.append(Component.text(spaces, textColor));
        }

        String dots = ".".repeat(width % 4 / 2);
        if (!dots.isEmpty()) {
            builder.append(Component.text(dots, textColor));
        }

        String one = Characters.ONE_LENGTH_DOT.repeat(width % 2);
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
        for (int c : string.codePoints().toArray()) {
            if (c == '-') {
                leadingSpaceWidth += 6;
            } else if (c == ' ') {
                leadingSpaceWidth += 4;
            } else break;
        }
        Component leadingSpace = pad(leadingSpaceWidth);

        Component out = null;
        Component line = null;

        int lineWidth = 0;
        System.out.println(Arrays.toString(string.split("(?= )", -1)));
        for (String part : string.split("(?= )")) {
            Component componentPart = ComponentHelper.legacy(part);
            int partWidth = width(componentPart);
            System.out.println(part+","+lineWidth+","+partWidth+","+wrapWidth);
            if (lineWidth > 0 && lineWidth + partWidth > wrapWidth) {
                out = appendLineTo(out, line, leadingSpace);
                line = null;
                lineWidth = 0;
            }
            lineWidth += partWidth;
            if (line == null) {
                line = componentPart;
            } else {
                line = line.append(componentPart);
            }
        }
        if (line != null) {
            out = appendLineTo(out, line, leadingSpace);
        }
        if (out == null) return Component.empty();
        return out.compact();
    }

    private static Component appendLineTo(Component out, Component line, Component leadingSpace) {
        line = line.replaceText(builder -> builder
                .once()
                .match(Pattern.compile("^[\\s-]*(.*)$"))
                .replacement((match, builder2) -> Component.text(match.group(1)))
        );
        if (out == null) {
            return line;
        } else {
            return out.appendNewline().append(leadingSpace).append(line);
        }
    }
}
