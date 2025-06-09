package dev.kshl.kshlib.kyori;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import javax.annotation.Nullable;

import java.util.Arrays;

public enum NamedTextColorChar {
    BLACK('0', NamedTextColor.BLACK, null),
    DARK_BLUE('1', NamedTextColor.DARK_BLUE, null),
    DARK_GREEN('2', NamedTextColor.DARK_GREEN, null),
    DARK_AQUA('3', NamedTextColor.DARK_AQUA, null),
    DARK_RED('4', NamedTextColor.DARK_RED, null),
    DARK_PURPLE('5', NamedTextColor.DARK_PURPLE, null),
    GOLD('6', NamedTextColor.GOLD, null),
    GRAY('7', NamedTextColor.GRAY, null),
    DARK_GRAY('8', NamedTextColor.DARK_GRAY, null),
    BLUE('9', NamedTextColor.BLUE, null),
    GREEN('a', NamedTextColor.GREEN, null),
    AQUA('b', NamedTextColor.AQUA, null),
    RED('c', NamedTextColor.RED, null),
    LIGHT_PURPLE('d', NamedTextColor.LIGHT_PURPLE, null),
    YELLOW('e', NamedTextColor.YELLOW, null),
    WHITE('f', NamedTextColor.WHITE, null),

    BOLD('l', null, TextDecoration.BOLD),
    STRIKETHROUGH('m', null, TextDecoration.STRIKETHROUGH),
    UNDERLINED('n', null, TextDecoration.UNDERLINED),
    ITALIC('o', null, TextDecoration.ITALIC),
    OBFUSCATED('k', null, TextDecoration.OBFUSCATED),

    RESET('r', null, null);

    public static final char COLOR_CHAR = '§';

    private final char c;
    private final NamedTextColor color;
    private final TextDecoration decoration;

    NamedTextColorChar(char c, NamedTextColor color, TextDecoration decoration) {
        this.c = c;
        this.color = color;
        this.decoration = decoration;
    }

    public static NamedTextColorChar getByChar(char c) {
        return Arrays.stream(values()).filter(color -> color.c == c).findAny().orElse(null);
    }

    public boolean isColor() {
        return c >= 'a' && c <= 'f' || c >= '0' && c <= '9';
    }

    public boolean isFormatButNotObfuscated() {
        return this == BOLD || this == STRIKETHROUGH || this == UNDERLINED || this == ITALIC;
    }

    public char getChar() {
        return c;
    }

    public @Nullable NamedTextColor getColor() {
        return color;
    }

    public @Nullable TextDecoration getDecoration() {
        return decoration;
    }
}
