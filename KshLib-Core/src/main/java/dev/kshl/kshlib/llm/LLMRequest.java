package dev.kshl.kshlib.llm;

import lombok.Getter;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.Duration;

public class LLMRequest {

    @Getter
    private final String model;
    @Getter
    private final String content;
    @Getter
    private String system;
    @Getter
    private boolean think;

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

    public LLMRequest system(@Nullable String system) {
        this.system = system;
        return this;
    }

    public LLMRequest think(boolean think) {
        this.think = think;
        return this;
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
        json.put("system", getSystem());
        json.put("think", isThink());
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
