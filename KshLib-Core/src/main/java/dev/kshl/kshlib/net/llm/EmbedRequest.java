package dev.kshl.kshlib.net.llm;

import org.json.JSONObject;

public class EmbedRequest extends LLMRequest {
    public EmbedRequest(String model, String input) {
        super(model, input);
    }

    @Override
    protected void putContent(JSONObject json) {
        json.put("input", getContent());
    }
}
