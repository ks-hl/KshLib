package dev.kshl.kshlib.parsing.equation.exception;

public class TimeoutException extends EvaluationException {
    public TimeoutException() {
        super("Maximum time exceeded."); // TODO show timeout
    }
}
