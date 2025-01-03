package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.awt.*;
import java.util.Objects;
import java.util.UUID;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class ComponentHelper {

    @Deprecated
    public static Component text(Object text) {
        return text(text, WHITE);
    }

    @Deprecated
    public static Component text(Object text, NamedTextColor color, TextDecoration... decorations) {
        return Component.text(Objects.toString(text)).color(color).decorate(decorations);
    }

    @Deprecated
    public static Component bracket(Object text, NamedTextColor contentColor, TextDecoration... contentDecoration) {
        return bracket(text, contentColor, DARK_GRAY, contentDecoration);
    }

    @Deprecated
    public static Component bracket(Object text, NamedTextColor contentColor, NamedTextColor bracketColor, TextDecoration... contentDecoration) {
        return bracket(text(text, contentColor, contentDecoration), bracketColor);
    }

    public static Component bracket(Component content) {
        return bracket(content, DARK_GRAY);
    }

    public static Component bracket(Component content, NamedTextColor bracketColor) {
        return concat(text("[", bracketColor), content, text("]", bracketColor));
    }

    @Deprecated
    public static Component quote(Object text, NamedTextColor contentColor, NamedTextColor quoteColor, TextDecoration... contentDecoration) {
        return quote(text(text, contentColor, contentDecoration), quoteColor);
    }

    public static Component quote(Component content, NamedTextColor quoteColor) {
        return surround(content, "\"", "\"", quoteColor);
    }

    public static Component quote(Component content) {
        return quote(content, DARK_GRAY);
    }

    public static Component parenthesis(Component content, NamedTextColor parenthesisColor) {
        return surround(content, "(", ")", parenthesisColor);
    }

    public static Component surround(Component content, String left, String right, NamedTextColor surroundColor) {
        return concat(text(left, surroundColor), content, text(right, surroundColor));
    }

    public static void appendSpace(TextComponent.Builder builder) {
        builder.append(space());
    }

    public static Component space() {
        return Component.text(" ");
    }

    public static Component legacy(String msg) {
        msg = msg.replace("§", "&");
        return LegacyComponentSerializer.builder().character('&').hexColors().build().deserialize(msg.replace("§", "&"));
    }

    public static Component concat(Component... components) {
        TextComponent.Builder builder = Component.text();
        for (Component component : components) {
            builder.append(component);
        }
        return builder.build();
    }

    public static String strip(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String strip(String text) {
        return strip(legacy(text));
    }

    public static String toHex(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static TextColor asTextColor(Color color) {
        return TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static void appendPrompt(TextComponent.Builder builder, UUID recipientUUID, Component text, String hover, String command) {
        if (recipientUUID == null || recipientUUID.getMostSignificantBits() == 0) {
            // Bedrock or console
            builder.append(Component.text(" ".repeat(10)));
            builder.append(text);
            builder.append(Component.text(" - " + command, NamedTextColor.GRAY));
            builder.appendNewline();
        } else {
            builder.append(Component.text(" ".repeat(10)));
            builder.append(bracket(text, DARK_GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text(hover).appendNewline().appendNewline().append(Component.text(command, NamedTextColor.GRAY))))
                    .clickEvent(ClickEvent.runCommand(command)));
        }
    }

    public static String replaceLegacyWithMini(String legacy) {
        Component component = ComponentHelper.legacy(legacy);
        return MiniMessage.miniMessage().serialize(component);
    }
}
