package dev.kshl.kshlib.net;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.kshl.kshlib.misc.StackUtil;
import dev.kshl.kshlib.misc.Timer;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public abstract class WebServer implements Runnable, HttpHandler {
    private final int maxRequestLength;
    private final RateLimiter globalRateLimiter;
    private final Map<String, RateLimiter> endpointSpecificRateLimiter = new HashMap<>();
    private final Set<String> origins;
    private final int numberOfProxiesToResolve;
    private int port;
    private final boolean requireJSONInput;
    private boolean closed;
    private HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public WebServer(int port, int numberOfProxiesToResolve, int maxRequestLength, boolean requireJSONInput, String... origins) {
        this(port, numberOfProxiesToResolve, maxRequestLength, new RateLimitParams(5, 5000L), requireJSONInput, origins);
    }

    private static Map<String, String> parseQuery(String query) throws WebException {
        if (query == null || query.isEmpty()) return Map.of();
        Map<String, String> params = new HashMap<>();
        for (String part : query.split("&")) {
            String[] kv = part.split("=");
            if (kv.length > 2)
                throw new WebException(HTTPResponseCode.BAD_REQUEST, "Improperly formatted query string");
            params.put(kv[0], kv.length == 2 ? kv[1] : null);
        }
        return params;
    }

    public WebServer(int port, int numberOfProxiesToResolve, int maxRequestLength, RateLimitParams globalRateLimitParams, boolean requireJSONInput, String... origins) {
        this.port = port;
        this.numberOfProxiesToResolve = numberOfProxiesToResolve;
        this.maxRequestLength = maxRequestLength;
        this.globalRateLimiter = new RateLimiter(null, globalRateLimitParams);
        this.requireJSONInput = requireJSONInput;

        this.origins = new HashSet<>(List.of(origins));
    }

    @Override
    public void run() {
        info("Starting web server on port " + port + "...");
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.port = server.getAddress().getPort();
        info("Web server running on port " + port);
        server.createContext("/", this);

        server.setExecutor(null);
        server.start();
    }

    public void close() {
        if (closed) return;
        closed = true;
        server.stop(0);
    }

    private String parseXForwardedFor(String sender, Headers headers) throws WebException {
        if (numberOfProxiesToResolve <= 0) return sender;
        if (headers == null) return sender;

        String cf = headers.getFirst("CF-Connecting-IP");
        if (cf != null) return cf.trim();

        String forwarded = headers.getFirst("X-Forwarded-For");
        if (forwarded==null || forwarded.isEmpty()) return sender;
        String[] forwardedFor = forwarded.split("\\s*,\\s*");
        if (forwardedFor.length < numberOfProxiesToResolve)
            throw new WebException(HTTPResponseCode.BAD_REQUEST, "Not enough X-Forwarded-For headers");
        return forwardedFor[forwardedFor.length - numberOfProxiesToResolve];
    }

    @Override
    public void handle(HttpExchange t) {
        final long requestTime = System.currentTimeMillis();
        executor.submit(() -> {
            Timer timer = new Timer(1);
            String sender = null;
            String endpoint = null;
            try {
                String requestString = null;
                Response response;
                Request request = null;
                String msg = null;

                try {
                    sender = parseXForwardedFor(t.getRemoteAddress().getAddress().toString(), t.getRequestHeaders());
                    if (sender.startsWith("/")) sender = sender.substring(1);
                    endpoint = Optional.ofNullable(t.getRequestURI().getPath()).orElse("/");
                    final String queryString = t.getRequestURI().getQuery();
                    msg = sender + " " + t.getRequestMethod() + " " + endpoint + (queryString != null ? "?" + queryString : "") + " - %d";

                    boolean global = globalRateLimiter.doRateLimit(sender, endpoint);
                    boolean specific = endpointSpecificRateLimiter.computeIfAbsent(endpoint.toLowerCase(), endpoint_ -> new RateLimiter(endpoint_, getRateLimitParameters(endpoint_))).doRateLimit(sender, endpoint);
                    if (global || specific) {
                        throw new WebException(HTTPResponseCode.TOO_MANY_REQUESTS);
                    }

                    requestString = new String(t.getRequestBody().readAllBytes());
                    if (requestString.isEmpty()) requestString = "{}";

                    var query = parseQuery(t.getRequestURI().getQuery());

                    if (requestString.length() > maxRequestLength) {
                        throw new WebException(HTTPResponseCode.PAYLOAD_TOO_LARGE);
                    } else {
                        JSONObject jsonIn = null;
                        try {
                            jsonIn = new JSONObject(requestString);
                        } catch (JSONException e) {
                            if (requireJSONInput) throw new WebException(HTTPResponseCode.BAD_REQUEST, "Invalid JSON");
                        }
                        response = handle(request = new Request(requestTime, sender, endpoint, HTTPRequestType.valueOf(t.getRequestMethod()), t.getRequestHeaders(), query, requestString, jsonIn));
                        if (response == null) throw new WebException(HTTPResponseCode.NOT_FOUND);
                    }
                } catch (WebException e) {
                    response = new Response().code(e.responseCode).body(new JSONObject().put("error", e.userErrorMessage));
                    msg += " - " + e.getMessage();
                } catch (Throwable e) {
                    response = new Response().code(HTTPResponseCode.INTERNAL_SERVER_ERROR).body(new JSONObject().put("error", "An unknown error occurred"));
                    msg += " - " + e.getMessage() + ": " + StackUtil.format(e, 0);
                }
                msg = String.format(msg, response.code) + ", took " + timer;
                logRequest(request, response, msg);

                if (response.eventStream == null && response.isJSON)
                    t.getResponseHeaders().add("Content-Type", "application/json");

                if (response.headers != null) {
                    for (Map.Entry<String, String> entry : response.headers.entrySet()) {
                        t.getResponseHeaders().add(entry.getKey(), entry.getValue());
                    }
                }
                if (!origins.isEmpty() && request != null) {
                    var originList = request.headers().get("Origin");
                    if (originList != null && !originList.isEmpty()) {
                        String origin = originList.get(0);
                        if (origins.contains(origin)) {
                            t.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
                            t.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
                        }
                    }
                }

                if (response.eventStream != null) {
                    t.getResponseHeaders().add("Content-Type", "text/event-stream");
                    t.getResponseHeaders().add("Cache-Control", "no-cache");
                    t.getResponseHeaders().add("Connection", "keep-alive");

                    t.sendResponseHeaders(response.code, 0);

                    AtomicReference<String> endCause = new AtomicReference<>();
                    try (OutputStream os = t.getResponseBody()) {
                        while (true) {
                            EventStream.Event event = response.eventStream.poll();
                            String body = event.format();
                            if (body != null) {
                                os.write(body.getBytes());
                                os.flush();
                            }
                            if (event.close()) {
                                endCause.set("Server aborted");
                                break;
                            }
                            try {
                                //noinspection BusyWait
                                Thread.sleep(response.frequency);
                            } catch (InterruptedException e) {
                                endCause.set("Thread interrupted");
                                return;
                            }
                        }
                    } catch (Throwable e) {
                        if (e instanceof IOException && e.getMessage() != null && e.getMessage().contains(" aborted ")) {
                            endCause.set("Client aborted");
                        } else {
                            endCause.set("Error: " + e.getClass().getName() + (e.getMessage() == null ? "" : (" - " + e.getMessage())));
                        }
                    } finally {
                        logRequest(request, response, "Stream ended @ " + request.endpoint() + ", cause: " + endCause.get());
                    }
                } else {

                    byte[] out = response.body == null ? new byte[0] : response.body.getBytes();
                    t.sendResponseHeaders(response.code, out.length);
                    try (OutputStream os = t.getResponseBody()) {
                        if (out.length > 0) os.write(out);
                        else os.write(new byte[]{0});
                    }
                }
            } catch (IOException e) {
                print("An error occurred while handling request from " + sender + " to " + endpoint, e);
            }
        });
    }

    public abstract void print(String msg, Throwable t);

    public abstract void info(String msg);

    public abstract void warning(String msg);

    protected abstract Response handle(Request request) throws WebException;

    public record Request(long requestTime, String sender, String endpoint, HTTPRequestType type,
                          @Nonnull Headers headers, @Nonnull Map<String, String> query,
                          @Nonnull String bodyString, @Nullable JSONObject bodyJSON) {
        @Nonnull
        public JSONObject bodyJSONOrEmpty() {
            return bodyJSON() == null ? new JSONObject() : bodyJSON();
        }
    }

    protected RateLimitParams getRateLimitParameters(@SuppressWarnings("unused") String endpoint) {
        return globalRateLimiter.params;
    }

    public static class WebException extends IOException {
        public final HTTPResponseCode responseCode;

        private final String userErrorMessage;

        public WebException(HTTPResponseCode responseCode) {
            this(responseCode, null, null);
        }

        public WebException(HTTPResponseCode responseCode, String userErrorMessage) {
            this(responseCode, userErrorMessage, userErrorMessage);
        }

        public WebException(HTTPResponseCode responseCode, String msg, String userErrorMessage) {
            super(responseCode.getCode() + " - " + responseCode.toStringCapitalized() + ((msg == null ? "" : (" - " + msg))));
            this.responseCode = responseCode;
            this.userErrorMessage = userErrorMessage == null ? responseCode.toStringCapitalized() : userErrorMessage;
        }

        public String getUserErrorMessage() {
            return userErrorMessage;
        }
    }

    public record RateLimitParams(int maxRequests, long withinMillis) {
    }

    public static class Response {
        int code = 200;
        String body;
        Map<String, String> headers;
        boolean isJSON;
        EventStream eventStream;
        private long frequency;

        public Response() {
        }

        public Response code(int code) {
            this.code = code;
            return this;
        }

        public Response code(HTTPResponseCode code) {
            this.code = code.getCode();
            return this;
        }

        public Response body(JSONObject json) {
            this.body = json.toString();
            this.isJSON = true;
            return this;
        }

        public Response body(String body) {
            this.body = body;
            this.isJSON = false;
            return this;
        }

        public Response headers(Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }

        public Response header(String key, String value) {
            if (this.headers == null) this.headers = new HashMap<>();
            headers.put(key, value);
            return this;
        }

        public Response eventStream(EventStream eventStream, long frequency) {
            this.eventStream = eventStream;
            this.frequency = frequency;
            return this;
        }
    }

    @FunctionalInterface
    public interface EventStream {
        Event poll() throws WebException;

        record Event(@Nullable String body, boolean close) {
            public String format() {
                if (body() == null) return null;
                return "data: " + body() + "\n\n";
            }
        }
    }

    protected class RateLimiter {
        private final Map<String, List<Long>> map = new HashMap<>();
        private long lastPurged = System.currentTimeMillis();
        private final String endpoint;
        private final RateLimitParams params;

        public RateLimiter(String endpoint, RateLimitParams params) {
            this.endpoint = endpoint;
            this.params = params;
        }

        public synchronized boolean doRateLimit(String sender, String endpoint) throws WebException {
            if (System.currentTimeMillis() - lastPurged > 100) {
                lastPurged = System.currentTimeMillis();
                map.values().forEach(set -> set.removeIf(time -> lastPurged - time > params.withinMillis()));
            }
            List<Long> times = map.computeIfAbsent(sender, o -> new ArrayList<>());
            times.add(System.currentTimeMillis());
            onRequest(sender, this.endpoint == null, endpoint, times.size());
            return times.size() > params.maxRequests();
        }
    }

    /**
     * Called twice for each request, one for global, one for endpoint-specific.
     *
     * @param sender           IP address of the sender.
     * @param isGlobalLimiter  Whether this call is for the global limiter or the endpoint-specific limiter.
     * @param endpoint         The endpoint requested.
     * @param numberOfRequests The number of requests, including this one, within the period of time specified by the applicable {@link RateLimitParams}
     * @throws WebException In order to disallow the connection for any reason.
     */
    protected void onRequest(String sender, boolean isGlobalLimiter, String endpoint, int numberOfRequests) throws WebException {
    }

    protected void logRequest(Request request, Response response, String msg) {
        if (response.code >= 400) warning(msg);
        else info(msg);
    }

    public final int getPort() {
        return port;
    }
}
