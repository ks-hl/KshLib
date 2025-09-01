package dev.kshl.kshlib.net;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class WebSocketClient implements WebSocket.Listener {
    private final String url;
    private WebSocket webSocket;

    private StringBuilder textBuffer = new StringBuilder();
    private final List<ByteBuffer> binaryBuffer = new ArrayList<>();
    private int binaryBufferTotalBytes = 0;

    public WebSocketClient(String url) {
        this.url = url;
    }

    public void connect(String... headers) {
        if (headers.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of headers, must be an even number, got " + headers.length);
        }
        try {
            close().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException | ExecutionException ignored) {
        }

        WebSocket.Builder builder = HttpClient.newHttpClient()
                .newWebSocketBuilder();
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        builder.buildAsync(URI.create(url), this)
                .thenAccept(webSocket -> this.webSocket = webSocket);
    }

    protected abstract void onText(String data);

    protected void sendText(String message) {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket not yet open", null);
        }
        webSocket.sendText(message, true);
    }

    @Override
    public final CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            handle(data, last);
        } catch (Throwable t) {
            print("Uncaught exception handling data: " + data, t);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public final CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        try {
            handle(data, last);
        } catch (Throwable t) {
        }
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    public void print(String message, @Nullable Throwable t) {
        System.out.println(message);
        if (t != null) t.printStackTrace();
    }

    private synchronized void handle(CharSequence data, boolean last) {
        textBuffer.append(data);
        if (!last) return;
        String message = textBuffer.toString();
        textBuffer = new StringBuilder();
        try {
            WebSocketClient.this.onText(message);
        } catch (Throwable t) {
            print("Uncaught exception handling data: " + message, t);
        }
    }

    private synchronized void handle(ByteBuffer data, boolean last) {
        binaryBuffer.add(data.slice());
        binaryBufferTotalBytes += data.remaining();
        if (!last) return;
        ByteBuffer complete = ByteBuffer.allocate(binaryBufferTotalBytes);
        for (ByteBuffer buffer : binaryBuffer) {
            complete.put(buffer);
        }
        binaryBuffer.clear();
        binaryBufferTotalBytes = 0;
        complete.flip();
        onBinary(complete);
    }

    public CompletableFuture<Void> close() {
        if (webSocket == null || webSocket.isInputClosed() || webSocket.isOutputClosed()) return CompletableFuture.completedFuture(null);

        return this.webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Normal Closure").thenApply(w -> null);
    }

    protected void onBinary(ByteBuffer data) {
    }
}
