package dev.kshl.kshlib.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public abstract class WebSocketClient implements WebSocket.Listener {
    private WebSocket webSocket;

    private StringBuilder textBuffer = new StringBuilder();
    private final List<ByteBuffer> binaryBuffer = new ArrayList<>();
    private int binaryBufferTotalBytes = 0;

    public WebSocketClient(String url) {
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .thenAccept(webSocket -> this.webSocket = webSocket);
    }

    protected abstract void onText(String data);

    protected void sendText(String message) {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket not yet open");
        }
        webSocket.sendText(message, true);
    }

    @Override
    public final CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            handle(data, last);
        } catch (Throwable t) {
            System.out.println("Uncaught exception handling data: " + data);
            t.printStackTrace();
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public final CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        try {
            handle(data, last);
        } catch (Throwable t) {
            System.out.println("Uncaught exception handling binary data");
            t.printStackTrace();
        }
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    private synchronized void handle(CharSequence data, boolean last) {
        textBuffer.append(data);
        if (!last) return;
        WebSocketClient.this.onText(textBuffer.toString());
        textBuffer = new StringBuilder();
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

    protected void onBinary(ByteBuffer data) {
    }
}
