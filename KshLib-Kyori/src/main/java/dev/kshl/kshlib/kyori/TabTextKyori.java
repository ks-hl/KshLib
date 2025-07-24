package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import dev.kshl.kshlib.misc.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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

    private static final String anyColorOrFormatPattern = "[&" + LegacyComponentSerializer.SECTION_CHAR + "][a-f0-9k-or]";

    public static List<Component> wrap(String string, int wrapWidth) {
        if (wrapWidth <= 0) {
            throw new IllegalArgumentException("Invalid width, must be >0");
        }

        Pattern spaceFormatSwap = Pattern.compile("(" + anyColorOrFormatPattern + ")+( +)");
        string = spaceFormatSwap.matcher(string).replaceAll(match -> match.group(2) + match.group(1));

        Pattern precedingSpacePattern = Pattern.compile("^(?:( *)(" + anyColorOrFormatPattern + ")*-)?( *)");
        Matcher precedingSpaceMatcher = precedingSpacePattern.matcher(string);
        int leadingSpaceWidth = 0;
        int leadingSpaceIndex = 0;
        if (precedingSpaceMatcher.find()) {
            leadingSpaceWidth += (int) (StringUtil.count(precedingSpaceMatcher.group(), ' ') * 4);
            leadingSpaceWidth += (int) (StringUtil.count(precedingSpaceMatcher.group(), '-') * 6);
            leadingSpaceIndex = precedingSpaceMatcher.group().length();
        }

        Component leadingSpace = pad(leadingSpaceWidth);

        List<Component> out = new ArrayList<>();
        Component line = null, lastComponent = Component.empty();
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

            Component spaceComponent = ComponentHelper.legacy(space);
            Component partComponent = ComponentHelper.legacy(part);

            int spaceWidth = width(spaceComponent);
            int partWidth = width(partComponent);

            if (lineWidth > 0 && lineWidth + spaceWidth + partWidth > wrapWidth) {
                addLineTo(out, line, leadingSpace);
                line = null;
                lineWidth = 0;
                spaceComponent = Component.empty();
                spaceWidth = 0;
            }

            if (line == null) {
                line = Component.empty();
            }
            line = line.append(lastComponent = spaceComponent.mergeStyle(lastComponent).mergeStyle(spaceComponent));
            line = line.append(lastComponent = partComponent.mergeStyle(lastComponent).mergeStyle(partComponent));
            lineWidth += spaceWidth + partWidth;

            firstPart = false;
        }

        addLineTo(out, line, leadingSpace);
        return out;
    }

    private static void addLineTo(List<Component> out, Component line, Component leadingSpace) {
        if (line == null) return;

        if (!out.isEmpty()) {
            line = leadingSpace.append(line);
        }

        if (Component.empty().equals(line)) line = Component.text(""); // Maintains empty line breaks

        out.add(line);
    }
}
