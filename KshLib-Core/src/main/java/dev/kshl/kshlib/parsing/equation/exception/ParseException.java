package dev.kshl.kshlib.parsing.equation.exception;

public class ParseException extends IllegalArgumentException {
    private final int pos;

    public ParseException(String msg, int pos) {
        super(msg);
        this.pos = pos;
    }

    public int getPos() {
        return pos;
    }
}
