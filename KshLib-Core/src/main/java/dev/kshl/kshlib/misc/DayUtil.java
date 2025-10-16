package dev.kshl.kshlib.misc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DayUtil {
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static int getDateInt(LocalDate date) {
        String s = date.format(YYYYMMDD);
        if (s.length() != 8) throw new IllegalArgumentException("Invalid date: " + date);
        return Integer.parseInt(s);
    }

    public static int getDateInt() {
        return getDateInt(LocalDate.now());
    }

    public int getDateInt(long millis) {
        return getDateInt(LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }
}
