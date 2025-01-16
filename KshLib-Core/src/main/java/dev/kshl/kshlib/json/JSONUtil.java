package dev.kshl.kshlib.json;

import dev.kshl.kshlib.misc.Formatter;
import dev.kshl.kshlib.misc.Objects2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JSONUtil {

    public static void putMoney(JSONObject json, String key, BigDecimal value) {
        json.put(key, Math.round(value.doubleValue() * 100) / 100D);
        String money = Formatter.toString(value.doubleValue(), 2, false, false);
        json.put(key + "_f", "$" + (money.endsWith(".00") ? money.substring(0, money.length() - 3) : money));
    }

    public static void putTime(JSONObject json, String key, long millis) {
        json.put(key, millis);
        if (millis <= 0) return;
        json.put(key + "_f", format(millis));
    }

    private static String format(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static int hashCode(JSONObject json) {
        return json.keySet().stream().map(json::get).map(o -> {
            if (o instanceof JSONObject json_) return hashCode(json_);
            if (o instanceof JSONArray jsonArray) return Objects2.hash(jsonArray.toList());
            return o.hashCode();
        }).reduce(0, (a, b) -> a ^ b);
    }

    public static boolean equals(JSONObject a, JSONObject b) {
        if (Objects.equals(a, b)) return true;
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        for (String key : a.keySet()) {
            if (!Objects.equals(a.get(key), b.opt(key))) return false;
        }
        return true;
    }

    public static Stream<Object> stream(JSONArray jsonArray) {
        return StreamSupport.stream(jsonArray.spliterator(), false);
    }
}
