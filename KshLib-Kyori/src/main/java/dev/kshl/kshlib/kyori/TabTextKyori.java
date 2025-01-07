package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import dev.kshl.kshlib.misc.TabText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.annotation.Nullable;

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

        return builder.build();
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

    static int width0(Component component) {
        String text = LegacyComponentSerializer.legacySection().serialize(component);
        return TabText.width(text);
    }
}
