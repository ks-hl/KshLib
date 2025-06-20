package dev.kshl.kshlib.net;

import dev.kshl.kshlib.concurrent.ConcurrentReference;
import dev.kshl.kshlib.exceptions.BusyException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public class NetUtilInterval {
    private final String endpoint;
    private final long minimumInterval;
    private final long maxWait;
    private final ConcurrentReference<Object> lock = new ConcurrentReference<>(new Object());
    private final boolean followRedirects;
    private long lastAPIRequest;
    private long cooldownUntil;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public NetUtilInterval(String endpoint, long minimumInterval) {
        this(endpoint, minimumInterval, 5000);
    }

    public NetUtilInterval(String endpoint, long minimumInterval, long maxWait) {
        this(endpoint, minimumInterval, maxWait, false);
    }

    public NetUtilInterval(String endpoint, long minimumInterval, long maxWait, boolean followRedirects) {
        this.followRedirects = followRedirects;
        endpoint = endpoint.trim();
        this.endpoint = endpoint;
        this.minimumInterval = minimumInterval;
        this.maxWait = maxWait;
    }

    @SuppressWarnings("unused")
    @Nonnull
    public NetUtil.Response getResponse(String endpointSuffix, String... headers) throws IOException, BusyException {
        return request(NetUtil.get(endpointSuffix, followRedirects, headers));
    }

    @SuppressWarnings("unused")
    @Nonnull
    public NetUtil.Response putResponse(String endpointSuffix, String body, String... headers) throws IOException, BusyException {
        return request(NetUtil.put(endpointSuffix, body, followRedirects, headers));
    }

    @SuppressWarnings("unused")
    @Nonnull
    public NetUtil.Response postResponse(String endpointSuffix, String body, String... headers) throws IOException, BusyException {
        return request(NetUtil.post(endpointSuffix, body, followRedirects, headers));
    }

    @SuppressWarnings("unused")
    @Nonnull
    public CompletableFuture<NetUtil.Response> getCompletable(String endpointSuffix, String... headers) {
        return requestCompletable(NetUtil.get(endpointSuffix, followRedirects, headers));
    }

    @SuppressWarnings("unused")
    @Nonnull
    public CompletableFuture<NetUtil.Response> putCompletable(String endpointSuffix, String body, String... headers) {
        return requestCompletable(NetUtil.put(endpointSuffix, body, followRedirects, headers));
    }

    @SuppressWarnings("unused")
    @Nonnull
    public CompletableFuture<NetUtil.Response> postCompletable(String endpointSuffix, String body, String... headers) {
        return requestCompletable(NetUtil.post(endpointSuffix, body, followRedirects, headers));
    }

    public NetUtil.Response request(NetUtil.Request request) throws BusyException, IOException {
        request.setURL(adaptSuffixAndRateLimit(request.getURL()));
        onRequest(request);
        var response = lock.functionThrowing(o -> request.request(), maxWait);
        onResponse(request, response);
        return response;
    }

    public CompletableFuture<NetUtil.Response> requestCompletable(NetUtil.Request request) {
        request.setURL(adaptSuffixAndRateLimit(request.getURL()));
        onRequest(request);
        CompletableFuture<NetUtil.Response> out = new CompletableFuture<>() {
            @Override
            public boolean complete(NetUtil.Response response) {
                onResponse(request, response);
                return super.complete(response);
            }
        };
        executor.submit(() -> {
            try {
                lock.consume(o -> request.requestCompletable(out), maxWait);
            } catch (BusyException e) {
                out.completeExceptionally(e);
            }
        });
        return out;
    }

    private void rateLimit() {
        long time = System.currentTimeMillis();
        long timeSinceLast = time - lastAPIRequest;
        try {
            if (timeSinceLast < minimumInterval) {
                Thread.sleep(minimumInterval - timeSinceLast);
            }
            time = System.currentTimeMillis();
            if (time < cooldownUntil) {
                Thread.sleep(cooldownUntil - time);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        lastAPIRequest = System.currentTimeMillis();
    }

    protected String adaptSuffixAndRateLimit(String suffix) {
        rateLimit();
        String url = endpoint;
        if (suffix == null) return endpoint;
        if (suffix.isEmpty() || suffix.startsWith("/")) return endpoint + suffix;
        if (!url.endsWith("/")) url += "/";
        return url + suffix;
    }

    public void onRequest(NetUtil.Request request) {
    }

    public void onResponse(NetUtil.Request request, NetUtil.Response response) {
    }

    public void setCooldownUntil(long until) {
        this.cooldownUntil = until;
    }

    /**
     * Downloads a file from a URL
     *
     * @param endpointSuffix    HTTP URL of the file to be downloaded
     * @param downloadDirectory path of the directory to save the file
     * @param fileNameResolver  A function which maps the filename resolved from the URL and request to the desired name.
     */
    @SuppressWarnings("unused")
    public File downloadFile(String endpointSuffix, File downloadDirectory, UnaryOperator<String> fileNameResolver) throws IOException {
        return downloadFile(endpointSuffix, downloadDirectory, fileNameResolver, false);
    }

    /**
     * Downloads a file from a URL
     *
     * @param endpointSuffix    HTTP URL of the file to be downloaded
     * @param downloadDirectory path of the directory to save the file
     */
    public File downloadFile(String endpointSuffix, File downloadDirectory, UnaryOperator<String> fileNameResolver, boolean followRedirects) throws IOException {
        String url = adaptSuffixAndRateLimit(endpointSuffix);
        return NetUtil.downloadFile(url, downloadDirectory, fileNameResolver, followRedirects);
    }
}
