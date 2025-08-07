package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MiniMessageBuilder {
    private final String string;
    private boolean allowHex, allowColors, allowShadow, allowBasicFormatting, allowObfuscation, allowClickEvent, allowHoverEvent, allowRainbow;
    private boolean stripDisallowed;
    private boolean replaceURL;

    public MiniMessageBuilder(String string) {
        this.string = string;
    }

    public MiniMessageBuilder allowHex(boolean allowHex) {
        this.allowHex = allowHex;
        return this;
    }

    public MiniMessageBuilder allowHex() {
        return allowHex(true);
    }

    public MiniMessageBuilder allowColors(boolean allowColors) {
        this.allowColors = allowColors;
        return this;
    }

    public MiniMessageBuilder allowColors() {
        return allowColors(true);
    }

    public MiniMessageBuilder allowShadow(boolean allowShadow) {
        this.allowShadow = allowShadow;
        return this;
    }

    public MiniMessageBuilder allowShadow() {
        return allowShadow(true);
    }

    public MiniMessageBuilder allowBasicFormatting(boolean allowBasicFormatting) {
        this.allowBasicFormatting = allowBasicFormatting;
        return this;
    }

    public MiniMessageBuilder allowBasicFormatting() {
        return allowBasicFormatting(true);
    }

    public MiniMessageBuilder allowObfuscation(boolean allowObfuscation) {
        this.allowObfuscation = allowObfuscation;
        return this;
    }

    public MiniMessageBuilder allowObfuscation() {
        return allowObfuscation(true);
    }

    public MiniMessageBuilder allowClickEvent(boolean allowClickEvent) {
        this.allowClickEvent = allowClickEvent;
        return this;
    }

    public MiniMessageBuilder allowClickEvent() {
        return allowClickEvent(true);
    }

    public MiniMessageBuilder allowHoverEvent(boolean allowHoverEvent) {
        this.allowHoverEvent = allowHoverEvent;
        return this;
    }

    public MiniMessageBuilder allowHoverEvent() {
        return allowHoverEvent(true);
    }

    public MiniMessageBuilder allowRainbow(boolean allowRainbow) {
        this.allowRainbow = allowRainbow;
        return this;
    }

    public MiniMessageBuilder allowRainbow() {
        return allowRainbow(true);
    }

    public MiniMessageBuilder stripDisallowed(boolean stripDisallowed) {
        this.stripDisallowed = stripDisallowed;
        return this;
    }

    public MiniMessageBuilder stripDisallowed() {
        return stripDisallowed(true);
    }

    public MiniMessageBuilder allowAllColors() {
        return allowColors().allowHex().allowShadow().allowRainbow();
    }

    public MiniMessageBuilder allowAllFormatting() {
        return allowBasicFormatting().allowObfuscation();
    }

    public MiniMessageBuilder allowAllEvents() {
        return allowClickEvent().allowHoverEvent();
    }

    public MiniMessageBuilder allowAll() {
        return allowAllEvents().allowAllColors().allowAllFormatting().replaceURL();
    }

    public MiniMessageBuilder replaceURL() {
        this.replaceURL = true;
        return this;
    }

    public Component build() {
        String message = ComponentHelper.replaceLegacyWithMini(this.string);

        TagResolver.Builder parseBuilder = TagResolver.builder();
        TagResolver.Builder stripBuilder = TagResolver.builder();

        // If allowColors and !allowHex, we need to strip anyway because there is no discrete resolver tag for hex
        if (!allowHex && (allowColors || stripDisallowed)) {
            message = message.replaceAll("</?(c(olou?r)?:)?#[a-fA-F0-9]{1,10}>", "");
        }
        if (allowShadow) {
            parseBuilder.resolver(StandardTags.shadowColor());
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.shadowColor());
        }
        if (allowColors || allowHex) {
            parseBuilder.resolver(StandardTags.color());
        }
        if (!allowColors && !allowHex && stripDisallowed) {
            stripBuilder.resolver(StandardTags.color());
        }
        if (allowBasicFormatting) {
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.BOLD));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.ITALIC));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.UNDERLINED));
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH));
        }
        if (allowObfuscation) {
            parseBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.decorations(TextDecoration.OBFUSCATED));
        }
        if (allowClickEvent) {
            parseBuilder.resolver(StandardTags.clickEvent());
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.clickEvent());
        }
        if (allowHoverEvent) {
            parseBuilder.resolver(StandardTags.hoverEvent());
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.hoverEvent());
        }
        if (allowRainbow) {
            parseBuilder.resolver(StandardTags.rainbow());
        } else if (stripDisallowed) {
            stripBuilder.resolver(StandardTags.rainbow());
        }
        parseBuilder.resolver(StandardTags.reset());

        if (stripDisallowed) {
            message = MiniMessage.builder().tags(stripBuilder.build()).build().stripTags(message);
        }
        Component out = MiniMessage.builder().tags(parseBuilder.build()).build().deserialize(message);
        if (replaceURL) {
            out = ComponentHelper.replaceURLs(out, true);
        }
        return out;
    }

    public String toPlainText() {
        return PlainTextComponentSerializer.plainText().serialize(build());
    }

    public String toMiniMessage() {
        return MiniMessage.miniMessage().serialize(build());
    }
}
