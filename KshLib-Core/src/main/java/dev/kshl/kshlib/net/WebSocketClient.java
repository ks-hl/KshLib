package dev.kshl.kshlib.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public abstract class WebSocketClient {
    private WebSocket webSocket;

    public WebSocketClient(String url) {
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), new ClientListener())
                .thenAccept(webSocket -> this.webSocket = webSocket);
    }

    protected void onOpen() {
    }

    protected abstract void onText(String data);

    protected void sendText(String message) {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket not yet open");
        }
        webSocket.sendText(message, true);
    }


    private class ClientListener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocketClient.this.onOpen();
            WebSocket.Listener.super.onOpen(webSocket);
        }

        StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                handle(data, last);
            } catch (Throwable t) {
                System.out.println(data);
                t.printStackTrace();
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        private synchronized void handle(CharSequence data, boolean last) {
            buffer.append(data);
            if (!last) return;
            WebSocketClient.this.onText(buffer.toString());
            buffer = new StringBuilder();
        }
    }
}
