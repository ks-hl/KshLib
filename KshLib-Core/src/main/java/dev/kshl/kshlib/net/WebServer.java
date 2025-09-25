package dev.kshl.kshlib.net;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.kshl.kshlib.misc.StackUtil;
import dev.kshl.kshlib.misc.Timer;
import dev.kshl.kshlib.sql.SQLAPIKeyManager;
import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public abstract class WebServer implements Runnable, HttpHandler {
    // CORS configuration
    private static final Set<String> ALLOWED_REQUEST_HEADERS = Set.of(
            "authorization", "content-type", "x-requested-with"
    );
    private static final long PREFLIGHT_MAX_AGE_SECONDS = 600; // 10 minutes

    private boolean isOriginAllowed(@Nullable String origin) {
        return origin != null && origins.contains(origin);
    }

    private final int maxRequestLength;
    private final RateLimiter rateLimiter;
    private final Set<String> origins;
    private final int numberOfProxiesToResolve;
    private int port;
    private final boolean requireJSONInput;
    private boolean closed;
    private HttpServer server;
    private final List<Object> endpoints = new ArrayList<>() {{
        add(WebServer.this);
    }};

    public WebServer(int port, int numberOfProxiesToResolve, int maxRequestLength, boolean requireJSONInput, String... origins) {
        this(port, numberOfProxiesToResolve, maxRequestLength, new RateLimiter(600, 600_000L), requireJSONInput, origins);
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

    public WebServer(int port, int numberOfProxiesToResolve, int maxRequestLength, RateLimiter rateLimiter, boolean requireJSONInput, String... origins) {
        this.port = port;
        this.numberOfProxiesToResolve = numberOfProxiesToResolve;
        this.maxRequestLength = maxRequestLength;
        this.rateLimiter = rateLimiter;
        this.requireJSONInput = requireJSONInput;

        this.origins = new HashSet<>(List.of(origins));
    }

    @Override
    @Deprecated
    public void run() {
        start();
    }

    public void start() {
        start(new InetSocketAddress(port));
    }

    public void start(InetSocketAddress inetSocketAddress) {
        info("Starting web server on port " + port + "...");
        try {
            server = HttpServer.create(inetSocketAddress, 0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.port = server.getAddress().getPort();
        info("Web server running on port " + port);
        server.createContext("/", this);

        server.setExecutor(Executors.newFixedThreadPool(8));
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
        if (forwarded == null || forwarded.isEmpty()) return sender;
        String[] forwardedFor = forwarded.split("\\s*,\\s*");
        if (forwardedFor.length < numberOfProxiesToResolve)
            throw new WebException(HTTPResponseCode.BAD_REQUEST, "Not enough X-Forwarded-For headers");
        return forwardedFor[forwardedFor.length - numberOfProxiesToResolve];
    }

    public void registerEndpoint(Object endpoint) {
        getEndpointAnnotatedMethods(endpoint); // Checks for incorrectly formatted @Endpoint methods
        endpoints.add(endpoint);
    }

    @Override
    public final void handle(HttpExchange t) {
        final long requestTime = System.currentTimeMillis();

        Timer timer = new Timer();
        String sender = null;
        String endpoint = null;
        try {
            String requestString;
            Response response;
            Request request = null;
            String msg = null;

            try {
                String originHeader = t.getRequestHeaders().getFirst("Origin");
                String method = t.getRequestMethod();

                // Short-circuit CORS preflight
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    String requestMethod = t.getRequestHeaders().getFirst("Access-Control-Request-Method");
                    String requestHeaders = t.getRequestHeaders().getFirst("Access-Control-Request-Headers");

                    Headers rh = t.getResponseHeaders();

                    if (isOriginAllowed(originHeader) && requestMethod != null) {
                        rh.add("Access-Control-Allow-Origin", originHeader);
                        rh.add("Access-Control-Allow-Credentials", "true");  // only if you truly need cookies/credentials
                        rh.add("Access-Control-Allow-Methods", allowedMethodsForEndpoint(
                                Optional.ofNullable(t.getRequestURI().getPath()).map(this::normalizeEndpointString).orElse("/")
                        ));

                        rh.add("Vary", "Origin");
                        rh.add("Vary", "Access-Control-Request-Headers");
                        rh.add("Vary", "Access-Control-Request-Method");
                        String allowedReqHeaders = filterRequestedHeaders(requestHeaders);
                        if (!allowedReqHeaders.isEmpty()) {
                            rh.add("Access-Control-Allow-Headers", allowedReqHeaders);
                        }

                        rh.add("Access-Control-Max-Age", Long.toString(PREFLIGHT_MAX_AGE_SECONDS));
                        // no body for preflight
                        t.sendResponseHeaders(204, -1);
                    } else {
                        // Origin not allowed or missing required headers → deny
                        t.sendResponseHeaders(403, -1);
                    }
                    return;
                }


                sender = parseXForwardedFor(t.getRemoteAddress().getAddress().toString(), t.getRequestHeaders());
                if (sender.startsWith("/")) sender = sender.substring(1);
                endpoint = Optional.ofNullable(t.getRequestURI().getPath()).map(this::normalizeEndpointString).orElse("/");
                final String queryString = t.getRequestURI().getQuery();
                msg = sender + " " + t.getRequestMethod() + " " + endpoint + (queryString != null ? "?" + queryString : "") + " - %d";

                {
                    boolean global;
                    synchronized (rateLimiter) {
                        global = rateLimiter.check(sender);
                    }
                    onRequest(sender, endpoint);
                    if (!global) {
                        throw new WebException(HTTPResponseCode.TOO_MANY_REQUESTS);
                    }
                }

                byte[] raw = readUpTo(t.getRequestBody(), maxRequestLength);
                requestString = new String(raw, StandardCharsets.UTF_8);
                if (requestString.isEmpty()) requestString = "{}";

                var query = parseQuery(t.getRequestURI().getQuery());

                JSONObject jsonIn = null;
                try {
                    jsonIn = new JSONObject(requestString);
                } catch (JSONException e) {
                    if (requireJSONInput) throw new WebException(HTTPResponseCode.BAD_REQUEST, "Invalid JSON");
                }
                request = new Request(requestTime, sender, endpoint, HTTPRequestType.valueOf(t.getRequestMethod()), t.getRequestHeaders(), query, requestString, jsonIn);

                response = handleEndpoints(request);
                if (response == null) response = handle(request);
                if (response == null) {
                    if (endpoint.equalsIgnoreCase("/favicon.ico")) {
                        response = new Response().code(HTTPResponseCode.NOT_FOUND); // 404 gracefully because favicon.ico is expected to be requested frequently during testing
                    } else {
                        throw new WebException(HTTPResponseCode.NOT_FOUND);
                    }
                }
            } catch (WebException e) {
                response = e.toResponse();
                msg += " - " + e.getMessage();
            } catch (Throwable e) {
                response = null;
                if (e instanceof Exception exception) {
                    try {
                        response = mapExceptionToResponse(exception);
                    } catch (WebException webException) {
                        response = webException.toResponse();
                    }
                }
                msg += " - " + e.getMessage();
                if (response == null) {
                    response = new Response().code(HTTPResponseCode.INTERNAL_SERVER_ERROR).body(new JSONObject().put("error", "An unknown error occurred"));
                    msg += ": " + StackUtil.format(e, 0);
                }
            }
            msg = String.format(msg, response.code) + ", took " + timer;
            logRequest(request, response, msg);

            if (response.eventStream == null && response.isJSON)
                t.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");

            if (response.headers != null) {
                for (Map.Entry<String, String> entry : response.headers.entrySet()) {
                    t.getResponseHeaders().add(entry.getKey(), entry.getValue());
                }
            }
            if (!origins.isEmpty() && request != null) {
                var originList = request.headers().get("Origin");
                String origin = (originList != null && !originList.isEmpty()) ? originList.get(0) : null;
                applyCorsForActualResponse(t.getResponseHeaders(), origin, true);
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
                            os.write(body.getBytes(StandardCharsets.UTF_8));
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
                    logRequest(request, response, "Stream ended @ " + endpoint + ", cause: " + endCause.get());
                }
            } else {

                byte[] out = response.body == null ? new byte[0] : response.body.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(response.code, out.length);
                try (OutputStream os = t.getResponseBody()) {
                    if (out.length > 0) os.write(out);
                    else os.write(new byte[]{0});
                }
            }
        } catch (Throwable e) {
            print("An error occurred while handling request from " + sender + " to " + endpoint, e);
        }
    }

    public @Nullable Response mapExceptionToResponse(Exception exception) throws WebException {
        return null;
    }

    /**
     * This method should only be called externally for testing.
     */
    public Response handleEndpoints(Request request) throws Throwable {
        for (Object endpointHandler : endpoints) {
            for (Map.Entry<Method, Endpoint> method : getEndpointAnnotatedMethods(endpointHandler).entrySet()) {
                boolean match = false;
                String endpointValue = method.getValue().value();
                if (method.getValue().regex()) {
                    match = request.endpoint().matches(endpointValue);
                } else {
                    for (String endpointValuePart : endpointValue.split(",")) {
                        endpointValuePart = normalizeEndpointString(endpointValuePart.trim());
                        if (request.endpoint().equalsIgnoreCase(endpointValuePart)) {
                            match = true;
                            break;
                        }
                    }
                }

                if (match) {
                    try {
                        return (Response) method.getKey().invoke(endpointHandler, request);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }

        return null;
    }

    private Map<Method, Endpoint> getEndpointAnnotatedMethods(Object endpointHandler) {
        Map<Method, Endpoint> methods = new LinkedHashMap<>();
        for (Method method : endpointHandler.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (!method.canAccess(endpointHandler)) continue;
            Endpoint endpointAnnotation = null;
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation instanceof Endpoint endpoint1) {
                    endpointAnnotation = endpoint1;
                    break;
                }
            }
            if (endpointAnnotation == null) continue;

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || !Objects.equals(parameters[0], Request.class))
                throw new IllegalArgumentException("Methods annotated with @Endpoint must accept exactly 1 argument, WebServer.Request");
            if (!Objects.equals(method.getReturnType(), Response.class))
                throw new IllegalArgumentException("Methods annotated with @Endpoint must return WebServer.Response");

            methods.put(method, endpointAnnotation);
        }
        return methods;
    }

    /**
     * Normalizes the endpoint string so that it has a leading '/' and no trailing '/'.
     */
    private String normalizeEndpointString(String endpoint) {
        endpoint = endpoint.trim();
        if (endpoint.equals("/")) return endpoint;
        if (endpoint.isBlank()) return "/";

        if (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        if (!endpoint.startsWith("/")) endpoint = "/" + endpoint;

        return endpoint;
    }

    private static byte[] readUpTo(InputStream in, int maxBytes) throws WebException, IOException {
        byte[] buf = in.readNBytes(maxBytes + 1);
        if (buf.length > maxBytes) throw new WebException(HTTPResponseCode.PAYLOAD_TOO_LARGE);
        return buf;
    }

    public abstract void print(String msg, Throwable t);

    public abstract void info(String msg);

    public abstract void warning(String msg);

    protected Response handle(Request request) throws WebException {
        return null;
    }

    private void applyCorsForActualResponse(Headers responseHeaders, @Nullable String origin, boolean allowCredentials) {
        if (!isOriginAllowed(origin)) return;

        responseHeaders.add("Access-Control-Allow-Origin", origin);
        if (allowCredentials) {
            responseHeaders.add("Access-Control-Allow-Credentials", "true");
        }
        responseHeaders.add("Vary", "Origin");
    }

    // Echo back only headers we allow (case-insensitive)
    private String filterRequestedHeaders(String acrh) {
        if (acrh == null || acrh.isBlank()) return "";
        String[] req = acrh.split("\\s*,\\s*");
        List<String> allowed = new ArrayList<>();
        for (String h : req) {
            if (ALLOWED_REQUEST_HEADERS.contains(h.toLowerCase())) {
                allowed.add(h);
            }
        }
        return String.join(", ", allowed);
    }

    // Which methods are allowed for this endpoint? (fallback to common verbs)
    private String allowedMethodsForEndpoint(String endpoint) {
        // Try to infer from annotated endpoints
        Set<String> methods = new HashSet<>();
        for (Object eh : endpoints) {
            for (Map.Entry<Method, Endpoint> e : getEndpointAnnotatedMethods(eh).entrySet()) {
                Endpoint ep = e.getValue();
                String pattern = ep.value();
                boolean matches;
                if (ep.regex()) {
                    matches = endpoint.matches(pattern);
                } else {
                    matches = false;
                    for (String part : pattern.split(",")) {
                        if (normalizeEndpointString(part.trim()).equalsIgnoreCase(endpoint)) {
                            matches = true;
                            break;
                        }
                    }
                }
                if (matches) methods.add(ep.method().name());
            }
        }
        if (methods.isEmpty()) methods.addAll(List.of("GET", "POST", "PUT", "DELETE")); // sensible default
        methods.add("OPTIONS");
        return String.join(", ", methods);
    }


    public record Request(long requestTime, String sender, String endpoint, HTTPRequestType type,
                          @Nonnull Headers headers, @Nonnull Map<String, String> query,
                          @Nonnull String bodyString, @Nullable JSONObject bodyJSON) {
        @Nonnull
        public JSONObject bodyJSONOrEmpty() {
            return bodyJSON() == null ? new JSONObject() : bodyJSON();
        }

        @Nullable
        public SQLAPIKeyManager.APIKeyPair getAPIKey() {
            String base64 = headers().getFirst("Authorization");
            if (base64 == null) return null;
            if (base64.startsWith("Basic ")) base64 = base64.substring(6);

            String key = new String(Base64.getDecoder().decode(base64));
            if (!key.matches("\\d+:[a-zA-Z0-9]+")) return null;
            String[] parts = key.split(":");
            int id = Integer.parseInt(parts[0]);
            return new SQLAPIKeyManager.APIKeyPair(id, parts[1]);
        }
    }

    @Getter
    public static class WebException extends IOException {
        private final HTTPResponseCode responseCode;
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

        public Response toResponse() {
            return new Response().code(getResponseCode()).body(new JSONObject().put("error", getUserErrorMessage()));
        }
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

    /**
     * Called twice for each request, one for global, one for endpoint-specific.
     *
     * @param sender   IP address of the sender.
     * @param endpoint The endpoint requested.
     * @throws WebException In order to disallow the connection for any reason.
     */
    protected void onRequest(String sender, String endpoint) throws WebException {
    }

    protected void logRequest(Request request, Response response, String msg) {
        if (response.code >= 400) warning(msg);
        else info(msg);
    }

    public final int getPort() {
        return port;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Endpoint {
        /**
         * The endpoint. May be multiple values separated by commas, or regex if the {@link #regex()} flag is set.
         * If the {@link #regex()} flag is set, commas are ignored and the entire string is considered one pattern.
         */
        String value() default "/";

        /**
         * The required request type
         */
        HTTPRequestType method() default HTTPRequestType.GET;

        /**
         * Whether to check the provided {@link #value()} with Regex. Commas within {@link #value()} are ignored if using regex.
         * Endpoints will always start with a leading '/' and have no trailing '/'. Default value handling ignores this, but regex is specific.
         */
        boolean regex() default false;
    }
}
