package dev.kshl.kshlib.net;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

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

    public static Request get(String url, String... headers) {
        return get(url, false, headers);
    }

    public static Request get(String url, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.GET, null, followRedirects, headers);
    }

    public static Request put(String url, String body, String... headers) {
        return put(url, body, false, headers);
    }

    public static Request put(String url, String body, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.PUT, body, followRedirects, headers);
    }

    public static Request post(String url, String body, String... headers) {
        return post(url, body, false, headers);
    }

    public static Request post(String url, String body, boolean followRedirects, String... headers) {
        return new Request(url, HTTPRequestType.POST, body, followRedirects, headers);
    }

    @SuppressWarnings("unused")
    public static final class Request {
        private String url;
        private final HTTPRequestType requestType;
        private final @Nullable String body;
        private final boolean followRedirects;
        private final String[] headers;

        public Request(String url, HTTPRequestType requestType, @Nullable String body, boolean followRedirects, String... headers) {
            this.url = url;
            this.requestType = requestType;
            this.body = body;
            this.followRedirects = followRedirects;
            this.headers = headers;
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

        @SuppressWarnings("unused")
        public Response request() throws IOException {
            Response response = request_();
            if (!followRedirects) return response;
            if (Set.of(301, 302, 307, 308).contains(response.getResponseCode().getCode())) {
                var locationList = response.headers().get("location");
                if (locationList == null || locationList.isEmpty()) locationList = response.headers().get("Location");
                if (locationList == null || locationList.isEmpty()) return response;
                String redirectTo = locationList.get(0);
                if (redirectTo.startsWith("/") || !redirectTo.startsWith("http")) {
                    if (!redirectTo.startsWith("/")) redirectTo = "/" + redirectTo;
                    redirectTo = url.replaceAll("(?<!/)/(?!/).*", "") + redirectTo;
                }
                if (redirectTo.equalsIgnoreCase(url)) return response;
                return new Request(url, requestType, body, true, headers).request();
            }
            return response;
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

        @Nonnull
        private Response request_() throws IOException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(getURL())).setHeader("User-Agent", "KSHLib");

            if (getHeaders() != null && getHeaders().length > 0) requestBuilder.headers(getHeaders());

            switch (getRequestType()) {
                case POST ->
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(getBody() == null ? "" : getBody()));
                case PUT -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(getBody() == null ? "" : getBody()));
                default -> {
                    if (getBody() != null)
                        throw new IllegalArgumentException("Cannot " + getRequestType() + " with a body");
                }
            }

            HttpRequest httpRequest = requestBuilder.build();
            HttpResponse<String> httpResponse;
            try {
                httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                throw new IOException("Request interrupted");
            }

            return new Response(httpResponse.headers().map(), httpResponse.statusCode(), httpResponse.body());
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

        public String[] getHeaders() {
            return headers;
        }
    }

    public static final class Response {
        private final Map<String, List<String>> headers;
        private final HTTPResponseCode responseCode;
        private final String body;
        private @Nullable JSONObject json;
        private boolean jsonResolved;

        public Response(Map<String, List<String>> headers, int response_code, String body) {
            this.headers = headers;
            this.responseCode = HTTPResponseCode.getByCode(response_code);
            this.body = body;
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
    }
}
