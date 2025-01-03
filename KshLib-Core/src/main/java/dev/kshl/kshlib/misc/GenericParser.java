package dev.kshl.kshlib.misc;

import java.util.function.Predicate;

public abstract class GenericParser {

    protected final String text;
    private final boolean skipSpaces;
    protected int pos, ch;

    public GenericParser(String text) {
        this(text, true);
    }

    public GenericParser(String text, boolean skipSpaces) {
        this.text = text;
        this.skipSpaces = skipSpaces;
        init();
    }

    protected void init() {
        pos = -1;
        ch = -1;
        nextChar();
    }

    private void fastForward(int amount) {
        ch = ((pos += amount) < text.length()) ? text.charAt(pos) : -1;
    }

    protected void nextChar() {
        fastForward(1);
    }

    protected boolean eat(String string) {
        if (skipSpaces) while (ch == ' ') nextChar();

        if (pos + string.length() > text.length()) return false;

        for (int i = 0; i < string.length(); i++) {
            char c = text.charAt(this.pos + i);
            if (c != string.charAt(i)) return false;
        }
        fastForward(string.length());
        return true;
    }

    protected boolean eat(int charToEat) {
        return eat(ch -> ch == charToEat);
    }

    protected boolean eat(int minCharToEat, int maxCharToEat) {
        return eat(ch -> ch >= minCharToEat && ch <= maxCharToEat);
    }

    protected boolean eat(Predicate<Character> charToEat) {
        if (skipSpaces) while (ch == ' ') nextChar();

        boolean eat = charToEat.test((char) ch);
        if (eat) nextChar();
        return eat;
    }

    protected String getUnexpectedCharacterMessage() {
        return "Unexpected character '" + ((char) ch) + "' at position " + pos;
    }

    protected boolean isDigit() {
        return isDigit((char) ch);
    }

    protected boolean isNumber() {
        return isNumber((char) ch);
    }

    protected boolean isLetter() {
        return isLetter((char) ch);
    }

    protected boolean isLetterOrUnderscore() {
        return isLetterOrUnderscore((char) ch);
    }

    protected static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    protected static boolean isNumber(char ch) {
        return isDigit(ch) || ch == '.';
    }

    protected static boolean isLetter(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
    }

    protected static boolean isLetterOrUnderscore(char ch) {
        return isLetter(ch) || ch == '_';
    }
}
