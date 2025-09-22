package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.function.ThrowingConsumer;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.misc.TimeUtil;
import dev.kshl.kshlib.net.HTTPRequestType;
import dev.kshl.kshlib.net.NetUtil;
import dev.kshl.kshlib.net.NetUtilInterval;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class OllamaAPI extends NetUtilInterval {

    public OllamaAPI(String url) {
        super(url, 50);
    }

    public LLMResponse generate(LLMRequest llmRequest) throws IOException {
        JSONObject responseJSON = request("api/generate", llmRequest);

        try {
            return LLMResponse.fromJSON(responseJSON);
        } catch (JSONException e) {
            throw new IOException("Invalid response: " + responseJSON, e);
        }
    }

    public EmbedResponse embeddings(EmbedRequest embedRequest) throws IOException {
        JSONObject responseJSON = request("api/embed", embedRequest);
        try {
            return EmbedResponse.fromJSON(responseJSON);
        } catch (JSONException e) {
            throw new IOException("Invalid response: " + responseJSON, e);
        }
    }

    private JSONObject request(String endpoint, LLMRequest llmRequest) throws IOException {
        NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit(endpoint), HTTPRequestType.POST, false);
        request.body(llmRequest.toJSON().toString());
        request.headers("content-type", "application/json");
        request.timeout(llmRequest.getTimeout());
        var response = request.request();
        if (response.getResponseCode().isError()) {
            throw new IOException(response.getResponseCode() + " (" + response.getResponseCode().getCode() + "): " + response.getBody());
        }
        String bodyResponse = response.getBody();
        try {
            return response.getJSON();
        } catch (JSONException e) {
            throw new IOException("Response not JSON. Body: " + bodyResponse, e);
        }
    }
}
