package dev.kshl.kshlib.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetUtil {
    public static final String CONTENT_TYPE_JSON = "application/json";
    private static final int BUFFER_SIZE = 4096;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Downloads a file from a URL
     *
     * @param url               HTTP URL of the file to be downloaded
     * @param downloadDirectory path of the directory to save the file
     * @param fileNameResolver  A function which maps the filename resolved from the URL and request to the desired name.
     */
    @SuppressWarnings("unused")
    public static File downloadFile(String url, File downloadDirectory, Function<String, String> fileNameResolver) throws IOException {
        return downloadFile(url, downloadDirectory, fileNameResolver, 0);
    }

    /**
     * Downloads a file from a URL
     *
     * @param url               HTTP URL of the file to be downloaded
     * @param downloadDirectory path of the directory to save the file
     */
    public static File downloadFile(String url, File downloadDirectory, Function<String, String> fileNameResolver, int loops) throws IOException {
        HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
        httpConn.addRequestProperty("User-Agent", "KSHLib");
        int statusCode = 0;
        httpConn.connect();
        try {
            statusCode = httpConn.getResponseCode();

            if (statusCode < 300) {
                String location = httpConn.getHeaderField("location");
                if (location != null && loops < 10) {
                    return downloadFile(location, downloadDirectory, fileNameResolver, loops + 1);
                }
                String fileName = null;
                String disposition = httpConn.getHeaderField("Content-Disposition");

                if (disposition != null) {
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 9);
                    }
                } else {
                    fileName = url.substring(url.lastIndexOf("/")).replace("/", "");
                }
                fileName = Objects.requireNonNull(fileNameResolver.apply(fileName));

                if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
                    throw new IOException("Failed to create download directory");
                }

                try (InputStream inputStream = httpConn.getInputStream()) {
                    File file = new File(downloadDirectory, fileName);
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        int bytesRead;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    return file;
                }
            } else {
                throw new IOException("HTTP code: " + statusCode);
            }
        } finally {
            httpConn.disconnect();
        }
    }

    @Nonnull
    public static JSONObject toJSON(String response) throws IOException {
        if (response.startsWith("[") && response.endsWith("]")) response = response.substring(1, response.length() - 1);
        try {
            return new JSONObject(response);
        } catch (JSONException ignored) {
            throw new IOException("Non-JSON getJSON: " + ((response.length() > 100) ? response.substring(0, 100) : response));
        }
    }


    @SuppressWarnings("unused")
    public static String getAuthHeader(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    @CheckReturnValue
    public static Request get(String url, String... headers) {
        return get(url, false, headers);
    }

    @CheckReturnValue
    public static Request get(String url, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.GET, null, followRedirects, headers);
    }

    @CheckReturnValue
    public static Request put(String url, String body, String... headers) {
        return put(url, body, false, headers);
    }

    @CheckReturnValue
    public static Request put(String url, String body, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.PUT, body, followRedirects, headers);
    }

    @CheckReturnValue
    public static Request post(String url, String body, String... headers) {
        return post(url, body, false, headers);
    }

    @CheckReturnValue
    public static Request post(String url, String body, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.POST, body, followRedirects, headers);
    }

    @SuppressWarnings("unused")
    public static final class Request {
        private String url;
        private final HTTPRequestType requestType;
        private @Nullable String body;
        private @Nullable byte[] bodyBytes;
        private final boolean followRedirects;
        private String[] headers;
        private Duration timeout;
        private Flow.Subscriber<String> streamSubscriber;
        private HttpResponse.BodyHandler<?> bodyHandler = HttpResponse.BodyHandlers.ofString();

        public Request(String url, HTTPRequestType requestType, @Nullable String body, boolean followRedirects, String... headers) {
            this.url = url;
            this.requestType = requestType;
            this.body = body;
            this.followRedirects = followRedirects;
            this.headers = headers;
        }

        public Request(String url, HTTPRequestType requestType, boolean followRedirects) {
            this.url = url;
            this.requestType = requestType;
            this.followRedirects = followRedirects;
        }

        public Request(String url, HTTPRequestType requestType, @Nullable String body, boolean followRedirects, Map<String, String> headers) {
            this(url, requestType, body, followRedirects, new String[headers.size() * 2]);

            int index = 0;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                this.headers[index++] = entry.getKey();
                this.headers[index++] = entry.getValue();
            }
        }

        public boolean doFollowRedirects() {
            return followRedirects;
        }

        public Response request() throws IOException {
            if (timeout == null) timeout = Duration.ofSeconds(3);

            HttpClient.Builder clientBuilder = HttpClient.newBuilder().connectTimeout(timeout);
            if (followRedirects) clientBuilder.followRedirects(HttpClient.Redirect.ALWAYS);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(getURL()));

            boolean hasUserAgent = false;
            if (getHeaders() != null) {
                for (int i = 0; i < getHeaders().length; i += 2) {
                    if (getHeaders()[i].equals("User-Agent")) {
                        hasUserAgent = true;
                        break;
                    }
                }
            }

            if (!hasUserAgent) requestBuilder.setHeader("User-Agent", "KshLib");

            if (getHeaders() != null && getHeaders().length > 0) requestBuilder.headers(getHeaders());

            Supplier<HttpRequest.BodyPublisher> publisherSupplier = () -> {
                if (getBodyBytes() != null) return HttpRequest.BodyPublishers.ofByteArray(getBodyBytes());
                return HttpRequest.BodyPublishers.ofString(getBody() == null ? "" : getBody());
            };

            switch (getRequestType()) {
                case POST -> requestBuilder.POST(publisherSupplier.get());
                case PUT -> requestBuilder.PUT(publisherSupplier.get());
                default -> {
                    if (getBody() != null)
                        throw new IllegalArgumentException("Cannot " + getRequestType() + " with a body");
                }
            }

            HttpClient client = clientBuilder.build();
            HttpRequest httpRequest = requestBuilder.build();
            HttpResponse<?> httpResponse;
            String body = null;
            byte[] bodyBytes = null;
            try {
                if (streamSubscriber == null) {
                    httpResponse = client.send(httpRequest, this.bodyHandler);
                    Object bodyObj = httpResponse.body();
                    if (bodyObj instanceof String string) {
                        body = string;
                    } else if (bodyObj instanceof byte[] bytes) {
                        bodyBytes = bytes;
                    }
                } else {
                    httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.fromLineSubscriber(streamSubscriber));
                }
            } catch (InterruptedException e) {
                throw new IOException("Request interrupted");
            }

            return new Response(httpResponse.headers().map(), httpResponse.statusCode(), body, bodyBytes);
        }

        public void requestCompletable(CompletableFuture<Response> completableFuture) {
            executor.submit(() -> {
                try {
                    completableFuture.complete(request());
                } catch (Throwable t) {
                    completableFuture.completeExceptionally(t);
                }
            });
        }

        public CompletableFuture<Response> requestCompletable() {
            CompletableFuture<Response> out = new CompletableFuture<>();
            requestCompletable(out);
            return out;
        }

        public String getURL() {
            return url;
        }

        public void setURL(String url) {
            this.url = url;
        }

        public HTTPRequestType getRequestType() {
            return requestType;
        }

        public @Nullable String getBody() {
            return body;
        }

        public @Nullable byte[] getBodyBytes() {
            return bodyBytes;
        }

        public String[] getHeaders() {
            return headers;
        }

        public Request timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Request headers(String... headers) {
            this.headers = headers;
            return this;
        }

        public Request appendHeaders(String... headers) {
            if (this.headers == null) return headers(headers);
            String[] oldHeaders = this.headers;
            this.headers = new String[this.headers.length + headers.length];
            System.arraycopy(oldHeaders, 0, this.headers, 0, oldHeaders.length);
            System.arraycopy(headers, 0, this.headers, oldHeaders.length, headers.length);

            return this;
        }

        public Request body(String body) {
            this.bodyBytes = null;
            this.body = body;
            return this;
        }

        public Request bodyBytes(byte[] body) {
            this.body = null;
            this.bodyBytes = body;
            return this;
        }

        public Request streamSubscriber(Flow.Subscriber<String> streamSubscriber) {
            this.streamSubscriber = streamSubscriber;
            return this;
        }


        public enum BodyType {STRING, BYTES}

        public Request setResponseBodyType(BodyType bodyType) {
            this.bodyHandler = switch (bodyType) {
                case STRING -> HttpResponse.BodyHandlers.ofString();
                case BYTES -> HttpResponse.BodyHandlers.ofByteArray();
            };
            return this;
        }
    }

    public static final class Response {
        private final Map<String, List<String>> headers;
        private final HTTPResponseCode responseCode;
        private final String body;
        private final byte[] bodyBytes;
        private @Nullable JSONObject json;
        private boolean jsonResolved;

        public Response(Map<String, List<String>> headers, int response_code, String body, byte[] bodyBytes) {
            this.headers = headers;
            this.responseCode = HTTPResponseCode.getByCode(response_code);
            this.body = body;
            this.bodyBytes = bodyBytes;
        }

        public Map<String, List<String>> headers() {
            return headers;
        }

        public JSONObject getJSON(JSONObject def) {
            return getJSON(def, false);
        }

        private JSONObject getJSON(JSONObject def, boolean doThrow) {
            if (jsonResolved) {
                if (json != null) return json;
                if (doThrow) throw new JSONException("Response is not valid JSON: " + getBody());
                return def;
            }
            jsonResolved = true;
            try {
                return json = new JSONObject(getBody());
            } catch (JSONException jsonException) {
                if (doThrow)
                    throw new JSONException(jsonException.getMessage() + ": " + getBody(), jsonException.getCause());
                return def;
            }
        }

        public HTTPResponseCode getResponseCode() {
            return responseCode;
        }

        public @Nonnull JSONObject getJSON() throws JSONException {
            return getJSON(null, true);
        }

        public String getBody() {
            return body;
        }

        public byte[] getBodyBytes() {
            return bodyBytes;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(int indent) {
            StringBuilder out = new StringBuilder();
            out.append("Response Code: ").append(getResponseCode()).append("\n");
            out.append("Headers: \n");
            for (Map.Entry<String, List<String>> entry : headers().entrySet()) {
                out.append(entry.getKey()).append(": ");
                for (String value : entry.getValue()) {
                    out.append(value).append(", ");
                }
                out.append("\n");
            }
            String body = getBody();
            if (indent > 0) {
                try {
                    body = new JSONObject(body).toString(indent);
                } catch (JSONException ignored) {
                    try {
                        body = new JSONArray(body).toString(indent);
                    } catch (JSONException ignored2) {
                    }
                }
            }
            if (bodyBytes != null) {
                out.append("Body Bytes Hex (").append(getBodyBytes().length).append("): ").append(HexFormat.of().formatHex(getBodyBytes())).append("\n");
            }
            if (getBody() != null) {
                out.append("Body Hex (").append(getBody().getBytes().length).append("): ").append(HexFormat.of().formatHex(body.getBytes())).append("\n");
            }
            out.append("Body: ").append(body);
            return out.toString();
        }
    }
}
