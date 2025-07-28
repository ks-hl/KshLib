package dev.kshl.kshlib.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RateLimiter {
    private final WebServer webServer;
    private final Map<String, List<Long>> map = new HashMap<>();
    private long lastPurged = System.currentTimeMillis();
    private final String endpoint;
    private final Params params;

    public RateLimiter(WebServer webServer, String endpoint, Params params) {
        this.webServer = webServer;
        this.endpoint = endpoint;
        this.params = params;
    }

    public synchronized boolean allow(String sender, String endpoint) throws WebServer.WebException {
        if (System.currentTimeMillis() - lastPurged > 100) {
            lastPurged = System.currentTimeMillis();
            map.values().forEach(set -> set.removeIf(time -> lastPurged - time > params.withinMillis()));
        }
        List<Long> times = map.computeIfAbsent(sender, o -> new ArrayList<>());
        times.add(System.currentTimeMillis());
        webServer.onRequest(sender, this.endpoint == null, endpoint, times.size());
        return times.size() > params.maxRequests();
    }

    public record Params(int maxRequests, long withinMillis) {
    }
}
