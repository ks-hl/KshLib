package dev.kshl.kshlib.net;

import dev.kshl.kshlib.exceptions.BusyException;
import javax.annotation.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.util.TimeZone;

public class TimeZoneAPI extends NetUtilInterval {

    public TimeZoneAPI() {
        super("https://ipapi.co/", 1000L);
    }

    @Nullable
    public TimeZone getTimeZoneForIP(String ip) throws IOException {
        JSONObject json;
        try {
            json = getResponse(ip + "/json/").getJSON();
        } catch (BusyException ignored) {
            return null;
        }
        if (!json.has("timezone")) return null;
        return TimeZone.getTimeZone(json.getString("timezone"));
    }
}
