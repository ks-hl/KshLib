package dev.kshl.kshlib.log;

public interface ILogger {
    void print(String explanation, Throwable t);

    void warning(String message);

    void info(String message);

    void debug(String message);
}
