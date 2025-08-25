package dev.kshl.kshlib.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public abstract class WebSocketClient implements WebSocket.Listener {
    private WebSocket webSocket;

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

    private StringBuilder textBuffer = new StringBuilder();

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

    private synchronized void handle(CharSequence data, boolean last) {
        textBuffer.append(data);
        if (!last) return;
        WebSocketClient.this.onText(textBuffer.toString());
        textBuffer = new StringBuilder();
    }
}
