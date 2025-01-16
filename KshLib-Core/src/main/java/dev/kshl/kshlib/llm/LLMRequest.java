package dev.kshl.kshlib.llm;

import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.Duration;

public class LLMRequest {

    private final String model;
    private final String content;

    private @Nullable Long seed;
    private @Nullable Integer contextLength;
    private @Nullable Duration timeout;

    public LLMRequest(String model, String content) {
        this.model = model;
        this.content = content;
    }

    public LLMRequest seed(@Nullable Long seed) {
        this.seed = seed;
        return this;
    }

    public LLMRequest contextLength(@Nullable Integer contextLength) {
        this.contextLength = contextLength;
        return this;
    }

    public LLMRequest timeout(@Nullable Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getModel() {
        return model;
    }

    public String getContent() {
        return content;
    }

    @Nullable
    public Long getSeed() {
        return seed;
    }

    @Nullable
    public Integer getContextLength() {
        return contextLength;
    }

    @Nullable
    public Duration getTimeout() {
        return timeout;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("stream", false);
        json.put("model", getModel());
        putContent(json);
        json.put("options", new JSONObject()
                .put("seed", getSeed())
                .put("num_ctx", getContextLength())
        );

        return json;
    }

    protected void putContent(JSONObject json) {
        json.put("prompt", getContent());
    }
}
