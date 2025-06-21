package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

public class MiniMessageBuilder {
    private final String string;
    private boolean allowHex, allowColors, allowShadow, allowBasicFormatting, allowObfuscation, allowClickEvent, allowHoverEvent, allowRainbow;

    public MiniMessageBuilder(String string) {
        this.string = string;
    }

    public MiniMessageBuilder allowHex(boolean allowHex) {
        this.allowHex = allowHex;
        return this;
    }

    public MiniMessageBuilder allowColors(boolean allowColors) {
        this.allowColors = allowColors;
        return this;
    }

    public MiniMessageBuilder allowShadow(boolean allowShadow) {
        this.allowShadow = allowShadow;
        return this;
    }

    public MiniMessageBuilder allowBasicFormatting(boolean allowBasicFormatting) {
        this.allowBasicFormatting = allowBasicFormatting;
        return this;
    }

    public MiniMessageBuilder allowObfuscation(boolean allowObfuscation) {
        this.allowObfuscation = allowObfuscation;
        return this;
    }

    public MiniMessageBuilder allowClickEvent(boolean allowClickEvent) {
        this.allowClickEvent = allowClickEvent;
        return this;
    }

    public MiniMessageBuilder allowHoverEvent(boolean allowHoverEvent) {
        this.allowHoverEvent = allowHoverEvent;
        return this;
    }

    public MiniMessageBuilder allowRainbow(boolean allowRainbow) {
        this.allowRainbow = allowRainbow;
        return this;
    }

    public Component build() {
        String message = ComponentHelper.replaceLegacyWithMini(this.string);

        TagResolver.Builder parseBuilder = TagResolver.builder();
        TagResolver.Builder stripBuilder = TagResolver.builder();

        if (!allowHex) {
            message = message.replaceAll("</?(c(olou?r)?:)?#[a-fA-F0-9]{1,10}>", "");
        }
        if (!allowShadow) {
            message = message.replaceAll("</?!?shadow(:[#a-zA-Z0-9_]+)*>", "");
        }
        if (allowColors || allowHex) {
            parseBuilder.resolver(StandardTags.color());
        }
        if (!allowColors && !allowHex) {
            stripBuilder.resolver(StandardTags.color());
        }
        if (allowBasicFormatting) {
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        } else {
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        }
        if (allowObfuscation) {
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        } else {
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        }
        if (allowClickEvent) {
            parseBuilder.resolver(StandardTags.clickEvent());
        } else {
            stripBuilder.resolver(StandardTags.clickEvent());
        }
        if (allowHoverEvent) {
            parseBuilder.resolver(StandardTags.hoverEvent());
        } else {
            stripBuilder.resolver(StandardTags.hoverEvent());
        }
        if (allowRainbow) {
            parseBuilder.resolver(StandardTags.rainbow());
        } else {
            stripBuilder.resolver(StandardTags.rainbow());
        }
        parseBuilder.resolver(StandardTags.reset());

        message = MiniMessage.builder().tags(stripBuilder.build()).build().stripTags(message);
        return MiniMessage.builder().tags(parseBuilder.build()).build().deserialize(message);
    }
}
