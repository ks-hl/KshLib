package dev.kshl.kshlib.net.discord;

import dev.kshl.kshlib.net.NetUtil;
import dev.kshl.kshlib.net.NetUtilInterval;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.CompletableFuture;

public class Webhook {
    private final NetUtilInterval webhook;
    private final String username;
    private final String avatar_url;

    public Webhook(String webhookURL) {
        this(webhookURL, null, null);
    }

    public Webhook(String webhookURL, String username, String avatar_url) {
        this.webhook = new NetUtilInterval(webhookURL, 500, 5000, false);
        this.username = username;
        this.avatar_url = avatar_url;
    }

    @CheckReturnValue
    public Builder builder() {
        return new Builder(username, avatar_url);
    }

    /**
     * <a href="https://discord.com/developers/docs/resources/webhook#execute-webhook">Discord Documentation</a>
     */
    public class Builder {
        private String content;
        private String username;
        private String avatar_url;
        private boolean tts;

        Builder(String username, String avatar_url) {
            this.username = username;
            this.avatar_url = avatar_url;
        }

        @CheckReturnValue
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        @CheckReturnValue
        public Builder avatar_url(String avatar_url) {
            this.avatar_url = avatar_url;
            return this;
        }

        @CheckReturnValue
        public Builder tts(boolean tts) {
            this.tts = tts;
            return this;
        }

        @CheckReturnValue
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public CompletableFuture<NetUtil.Response> send() {
            JSONObject out = new JSONObject();
            out.put("content", content);
            out.put("username", username);
            out.put("avatar_url", avatar_url);
            out.put("tts", tts);
            return webhook.postCompletable("", out.toString(), "Content-Type", "application/json");
        }
    }
}
