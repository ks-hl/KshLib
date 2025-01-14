package dev.kshl.kshlib.net;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class OllamaAPI extends NetUtilInterval {

    public OllamaAPI(String url) {
        super(url, 50);
    }

    public String chat(LLMRequest llmRequest) throws IOException {
        String bodyResponse = null;
        try {
            JSONObject json = new JSONObject();
            json.put("stream", false);
            json.put("model", llmRequest.getModel());
            json.put("prompt", llmRequest.getContent());
            json.put("options", new JSONObject()
                    .put("seed", llmRequest.getSeed())
                    .put("num_ctx", llmRequest.getContextLength())
            );
            NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit("api/generate"), HTTPRequestType.POST, false);
            request.body(json.toString());
            request.headers("content-type", "application/json");
            request.timeout(llmRequest.getTimeout());
            var response = request.request();
            if (response.getResponseCode().isError()) {
                throw new IOException(response.getResponseCode() + " (" + response.getResponseCode().getCode() + "): " + response.getBody());
            }
            bodyResponse = response.getBody();
            JSONObject responseJSON = response.getJSON();
            if (!responseJSON.has("response")) {
                throw new IOException("Invalid response: " + responseJSON);
            }
            return responseJSON.getString("response");
        } catch (JSONException e) {
            throw new JSONException("Body: " + bodyResponse, e);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new OllamaAPI("http://10.0.70.105:11434/").chat(new LLMRequest("qwen2.5:3b",
                "What's your favorite color?")));
    }
}
