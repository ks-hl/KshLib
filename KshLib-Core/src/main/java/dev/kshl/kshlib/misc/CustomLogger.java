package dev.kshl.kshlib.misc;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CustomLogger {

    public static Logger getLogger(String name, UnaryOperator<String> censor, @Nullable File file) {

        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new CustomFormatter(censor, true, true));
        logger.addHandler(handler);

        if (file != null) {
            try {
                boolean ignored = file.getParentFile().mkdirs();
                FileHandler fh = new FileHandler(file.getAbsolutePath());
                fh.setFormatter(new CustomFormatter(censor, false, true));
                logger.addHandler(fh);
            } catch (IOException e) {
                print(logger, "Failed to initialize File logger", e);
                System.exit(0);
            }
        }

        return logger;
    }

    public static void print(Logger logger, Level level, String message, Throwable t) {
        if (message == null) message = "";
        else message += ": ";
        message += t.getMessage();
        logger.log(level, message, t);
    }

    public static void print(Logger logger, String message, Throwable t) {
        print(logger, Level.WARNING, message, t);
    }

    public static void setDebug(Logger logger, boolean debug) {
        Level level = debug ? Level.ALL : Level.INFO;
        logger.setLevel(level);
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(level);
        }
        logger.log(Level.INFO, "Debug " + (debug ? "enabled" : "disabled"));
    }

    public static class CustomFormatter extends SimpleFormatter {
        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_RED = "\u001B[31m";
        public static final String ANSI_YELLOW = "\u001B[33m";
        public static final String ANSI_CYAN = "\u001B[96m";
        public static final String ANSI_WHITE = "\u001B[37m";
        private final String format;
        private final UnaryOperator<String> censor;
        private final boolean useColor;

        public CustomFormatter(UnaryOperator<String> censor, boolean useColor, boolean time) {
            this.censor = censor;
            this.useColor = useColor;
            this.format = (time ? "[%1$tF %1$tT.%2$s] " : "") + "[%3$s] %4$s %n";
        }

        @Override
        public synchronized String format(LogRecord record) {
            String color;
            String levelLabel = record.getLevel().getLocalizedName();
            if (useColor) {
                int level = record.getLevel().intValue();
                if (level >= Level.SEVERE.intValue()) {
                    color = ANSI_RED;
                } else if (level >= Level.WARNING.intValue()) {
                    color = ANSI_YELLOW;
                } else if (level >= Level.INFO.intValue()) {
                    color = ANSI_CYAN;
                } else {
                    color = ANSI_WHITE;
                    levelLabel = switch (record.getLevel().toString()) {
                        case "FINE" -> "DEBUG";
                        case "FINER" -> "VERBOSE";
                        case "FINEST" -> "SPAM";
                        default -> record.getLevel().getLocalizedName();
                    };
                }
            } else {
                color = "";
            }
            String millis = String.valueOf(record.getMillis() % 1000);
            millis = "0".repeat(3 - millis.length()) + millis;
            // Adjusted to fit the corrected format string with four placeholders
            String line = color + String.format(format, new Date(record.getMillis()), millis, levelLabel, record.getMessage());
            if (record.getThrown() != null) {
                line += " " + StackUtil.format(record.getThrown(), 0) + "\n";
            }
            return censor.apply(line) + (useColor ? ANSI_RESET : "");
        }
    }

    public static class ConsumerHandler extends Handler {
        private final CustomFormatter formatter;
        private final Consumer<Record> consumer;

        public ConsumerHandler(UnaryOperator<String> censor, Consumer<Record> consumer) {
            this.formatter = new CustomFormatter(censor, false, false);
            this.consumer = consumer;
        }

        @Override
        public void publish(LogRecord record) {
            String line = formatter.format(record);
            consumer.accept(new Record(record.getMillis(), TimeUtil.format(record.getMillis(), DateTimeFormatter.ISO_LOCAL_DATE_TIME, TimeZone.getDefault().toZoneId()), line, record.getLevel()));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public record Record(long timeMillis, String isoTime, String message, Level level) {
        }
    }
}
