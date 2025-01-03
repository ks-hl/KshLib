package dev.kshl.kshlib.misc;

import javax.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    public static final DateTimeFormatter ENTRY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm:ss.SSS z");
    @Deprecated
    public static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    public static final DateTimeFormatter FILE_FORMAT2 = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = SECOND_MILLIS * 60;
    public static final long HOUR_MILLIS = MINUTE_MILLIS * 60;
    public static final long DAY_MILLIS = HOUR_MILLIS * 24;

    /**
     * Use {@link Formatter#toString(double, int, boolean, boolean)}
     */
    @Deprecated
    public static double roundToPlaces(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    public static String millisToString(double millis) {
        if (millis < 1000) {
            int significance = 0;
            long factor = 1;
            while (millis * factor < 1 && significance < 3) {
                significance++;
                factor *= 1000;
            }
            String unit = switch (significance) {
                case 0 -> "ms";
                case 1 -> "µs";
                case 2 -> "ns";
                case 3 -> "ps";
                default -> throw new IllegalArgumentException();
            };
            return Formatter.toString(millis * factor, 2, true, true) + unit;
        }
        long factor;
        String unit;
        if (millis >= DAY_MILLIS) {
            factor = DAY_MILLIS;
            unit = "d";
        } else if (millis >= HOUR_MILLIS) {
            factor = HOUR_MILLIS;
            unit = "h";
        } else if (millis >= MINUTE_MILLIS) {
            factor = MINUTE_MILLIS;
            unit = "m";
        } else {
            factor = SECOND_MILLIS;
            unit = "s";
        }
        return Formatter.toString(millis / factor, 2, true, true) + unit;
    }

    public static String millisToStringExtended(long time) {
        return millisToStringExtended(time, false);
    }

    public static String millisToStringExtended(long time, boolean ms) {
        long days = time / DAY_MILLIS;
        long hours = time / HOUR_MILLIS % 24;
        long minutes = time / MINUTE_MILLIS % 60;
        long seconds, millis;
        if (ms) {
            seconds = time / SECOND_MILLIS % 60;
            millis = time % SECOND_MILLIS;
        } else {
            seconds = Math.round(time / (double) SECOND_MILLIS % 60);
            millis = 0;
        }
        String playtimeMsg = "";
        if (days > 0) {
            playtimeMsg += days + "d";
        }
        if (hours > 0) {
            playtimeMsg += " " + hours + "h";
        }
        if (minutes > 0) {
            playtimeMsg += " " + minutes + "m";
        }
        if (seconds > 0 || (playtimeMsg.isBlank() && !ms)) {
            playtimeMsg += " " + seconds + "s";
        }
        if (millis > 0 || (playtimeMsg.isBlank() && ms)) {
            playtimeMsg += " " + millis + "ms";
        }
        return playtimeMsg.trim();
    }

    public static long stringToMillis(String timeStr) throws NumberFormatException {
        return new TimeParser(timeStr).parse();
    }

    private static class TimeParser extends GenericParser {
        public TimeParser(String time) {
            super(time);
        }

        public long parse() {
            if (text.matches("\\d+e")) {
                return Long.parseLong(text.substring(0, text.length() - 1));
            }
            init();

            long time = 0;
            while (ch > 0) {
                time += parseTime(parseNumber());
            }
            return time;
        }

        private double parseNumber() {
            int startPos = pos;
            //noinspection StatementWithEmptyBody
            while (eat(GenericParser::isNumber)) ;
            if (ch < 0) {
                throw new NumberFormatException("Time without units at end");
            }
            if (pos == startPos) {
                throw new NumberFormatException(getUnexpectedCharacterMessage());
            }
            String time = text.substring(startPos, pos);
            return Double.parseDouble(time);
        }

        private long parseTime(double number) {
            if (eat("ms")) return Math.round(number);
            if (eat('s')) return Math.round(number * SECOND_MILLIS);
            if (eat('m')) return Math.round(number * MINUTE_MILLIS);
            if (eat('h')) return Math.round(number * HOUR_MILLIS);
            if (eat('d')) return Math.round(number * DAY_MILLIS);
            if (eat('w')) return Math.round(number * DAY_MILLIS * 7);

            throw new NumberFormatException(getUnexpectedCharacterMessage());
        }
    }

    public static String format(long millis, String format) {
        return format(millis, DateTimeFormatter.ofPattern(format));
    }

    public static String format(long millis, DateTimeFormatter formatter) {
        return format(millis, formatter, null);
    }

    public static String format(long millis, DateTimeFormatter formatter, @Nullable ZoneId timeZone) {
        if (timeZone == null) timeZone = ZoneId.systemDefault();
        return Instant.ofEpochMilli(millis).atZone(timeZone).format(formatter);
    }

    public static long parseToMillis(String date, DateTimeFormatter formatter) {
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
