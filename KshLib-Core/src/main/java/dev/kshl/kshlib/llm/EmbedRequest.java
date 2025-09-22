package dev.kshl.kshlib.llm;

import lombok.Getter;
import org.json.JSONObject;

import javax.annotation.Nullable;

@Getter
public abstract class EmbedRequest extends LLMRequest {
    private boolean truncate;

    public EmbedRequest(String model, String input) {
        super(model, input);
    }

    @Override
    protected void putContent(JSONObject json) {
        json.put("input", getContent());
    }

    public EmbedRequest truncate(boolean truncate) {
        this.truncate = truncate;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("truncate", isTruncate());
        return json;
    }

    @Override
    public LLMRequest think(boolean think) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LLMRequest system(@Nullable String system) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LLMRequest seed(@Nullable Long seed) {
        throw new UnsupportedOperationException();
    }
}
