package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class ComponentHelper {
    private static final Pattern HEX_PATTERN = Pattern.compile("&(#[a-fA-F0-9]{6})");
    private static final Pattern COLOR_PATTERN = Pattern.compile("&(([a-fA-F0-9])|(#[a-fA-F0-9]{6}))");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("&([mnlokr])");

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
        String msg = PlainTextComponentSerializer.plainText().serialize(component);
        msg = msg.replaceAll("[§&][a-fA-F0-9lmnorkLMNORK]", "");
        return msg;
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
        legacy = legacy.replace("§", "&");
        Matcher matcher = COLOR_PATTERN.matcher(legacy);
        StringBuilder text = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            text.append(parseLegacyFormat(legacy.substring(last, matcher.start())));
            String match = matcher.group(1);
            if (match.length() == 1) {
                text.append("<").append(NamedTextColorChar.getByChar(match.charAt(0)).toString().toLowerCase()).append(">");
            } else {
                text.append("<").append(match).append(">");
            }
            last = matcher.end();
        }
        if (last < legacy.length()) {
            text.append(parseLegacyFormat(legacy.substring(last)));
        }
        matcher = HEX_PATTERN.matcher(text);
        return matcher.replaceAll(match -> "<" + match.group(1) + ">");
    }

    private static String parseLegacyFormat(String string) {
        StringBuilder end = new StringBuilder();

        Matcher matcher = FORMAT_PATTERN.matcher(string);
        boolean containsReset = false;
        while (matcher.find()) {
            NamedTextColorChar format = NamedTextColorChar.getByChar(matcher.group(1).charAt(0));
            if (format == NamedTextColorChar.RESET) {
                containsReset = true;
            } else {
                end.insert(0, "</" + format.toString().toLowerCase() + ">");
            }
        }
        string = FORMAT_PATTERN.matcher(string).replaceAll(match -> "<" + NamedTextColorChar.getByChar(match.group(1).charAt(0)).toString().toLowerCase() + ">");
        if (!containsReset) string += end;
        return string;
    }

    @Deprecated
    public static Component parseMiniMessage(String message, boolean allowHex, boolean allowColors, boolean allowBasicFormatting, boolean allowObfuscation, boolean allowClickEvent, boolean allowHoverEvent, boolean allowRainbow) {
        message = ComponentHelper.replaceLegacyWithMini(message);
        var tagBuilder = TagResolver.builder();
        if (!allowHex && allowColors) {
            message = message.replaceAll("</?(c(olou?r)?:)?#[a-fA-F0-9]{1,10}>", "");
        }
        if (allowColors || allowHex) {
            tagBuilder.resolver(StandardTags.color());
        }
        if (allowBasicFormatting) {
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        }
        if (allowObfuscation) {
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        }
        if (allowClickEvent) {
            tagBuilder.resolver(StandardTags.clickEvent());
        }
        if (allowHoverEvent) {
            tagBuilder.resolver(StandardTags.hoverEvent());
        }
        if (allowRainbow) {
            tagBuilder.resolver(StandardTags.rainbow());
        }
        tagBuilder.resolver(StandardTags.reset());
        return MiniMessage.builder().tags(tagBuilder.build()).build().deserialize(message);
    }

    @Deprecated
    public static String stripMiniMessage(String message, boolean stripHex, boolean stripColors, boolean stripBasicFormatting, boolean stripObfuscation, boolean stripClickEvent, boolean stripHoverEvent, boolean stripRainbow) {
        message = ComponentHelper.replaceLegacyWithMini(message);
        var tagBuilder = TagResolver.builder();
        if (stripHex || stripColors) {
            message = message.replaceAll("</?(c(olou?r)?:)?#[a-fA-F0-9]{1,10}>", "");
        }
        if (stripColors) {
            tagBuilder.resolver(StandardTags.color());
        }
        if (stripBasicFormatting) {
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        }
        if (stripObfuscation) {
            tagBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        }
        if (stripClickEvent) {
            tagBuilder.resolver(StandardTags.clickEvent());
        }
        if (stripHoverEvent) {
            tagBuilder.resolver(StandardTags.hoverEvent());
        }
        if (stripRainbow) {
            tagBuilder.resolver(StandardTags.rainbow());
        }
        return MiniMessage.builder().tags(tagBuilder.build()).build().stripTags(message);
    }

    public static TextColor parseColor(String arg) throws IllegalArgumentException {
        arg = arg.replace("§", "&");
        if (arg.startsWith("&")) arg = arg.substring(1);
        if (arg.length() == 1) {
            return NamedTextColorChar.getByChar(arg.charAt(0)).getColor();
        }
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length() - 1);
        if (arg.startsWith("#")) {
            return Optional.ofNullable(TextColor.fromHexString(arg)).orElse(WHITE);
        }
        return Optional.ofNullable(NamedTextColorChar.getByName(arg)).map(NamedTextColorChar::getColor).orElse(WHITE);
    }

    public static TextColor[] parseColors(String[] args) throws IllegalArgumentException {
        TextColor[] out = new TextColor[args.length];
        for (int i = 0; i < args.length; i++) out[i] = parseColor(args[i]);
        return out;
    }

    public static final Pattern URL_PATTERN_HTTP_HTTPS = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    public static final Pattern URL_PATTERN_NO_HTTP = Pattern.compile("[-a-zA-Z0-9@:%._+~#=]{1,256}\\.(com?|net|org|us|xyz|uk|ca|cc)\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    public static final Pattern URL_PATTERN = Pattern.compile(URL_PATTERN_HTTP_HTTPS.pattern() + "|" + URL_PATTERN_NO_HTTP);

    public static Component replaceURLs(Component component, boolean requireHttpHttps) {
        return component.replaceText(TextReplacementConfig.builder()
                .match(requireHttpHttps ? URL_PATTERN_HTTP_HTTPS : URL_PATTERN)
                .replacement((match, builder) -> {
                    String url = match.group();
                    String urlLower = url.toLowerCase();
                    if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    return Component.text(match.group()).decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to open link\n" + url, NamedTextColor.GRAY)))
                            .clickEvent(ClickEvent.openUrl(url));
                })
                .build());
    }
}
