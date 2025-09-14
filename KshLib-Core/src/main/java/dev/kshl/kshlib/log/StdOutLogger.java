package dev.kshl.kshlib.log;

public class StdOutLogger implements ILogger {
    @Override
    public void print(String explanation, Throwable t) {
        System.err.println(explanation);
        //noinspection CallToPrintStackTrace
        t.printStackTrace();
    }

    @Override
    public void warning(String message) {
        System.err.println(message);
    }

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void debug(String message) {
        System.out.println(message);
    }
}
