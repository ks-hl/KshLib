package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.net.HTTPRequestType;
import dev.kshl.kshlib.net.NetUtil;
import dev.kshl.kshlib.net.NetUtilInterval;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class OpenWebUIAPI extends NetUtilInterval {

    private static final String FORMAT = "";
    private final String url;
    private final String key;

    public OpenWebUIAPI(String url) {
        super(url, 50);
        this.url = url;
        this.key = Dotenv.load().get("OPEN_WEB_UI_KEY");

        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key is null/empty");
    }

    public JSONMessage chat(LLMRequest llmRequest) throws IOException {
        String bodyResponse = null;
        try {
            JSONObject json = new JSONObject();
            json.put("stream", false);
            json.put("model", llmRequest.getModel());
            json.put("messages", new JSONArray().put(new JSONMessage("user", llmRequest.getContent())));
            json.put("options", new JSONObject()
                    .put("seed", llmRequest.getSeed())
                    .put("num_ctx", llmRequest.getContextLength())
            );
            NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit("ollama/api/chat"), HTTPRequestType.POST, false);
            request.body(json.toString());
            request.headers(
                    "authorization", "Bearer " + key,
                    "cookie", "token=" + key,
                    "content-type", "application/json"
            );
            request.timeout(llmRequest.getTimeout());
            var response = request.request();
            if (response.getResponseCode().isError()) {
                throw new IOException(response.getResponseCode() + " (" + response.getResponseCode().getCode() + "): " + response.getBody());
            }
            bodyResponse = response.getBody();
            JSONObject responseJSON = response.getJSON();
            if (!responseJSON.has("message")) {
                throw new IOException("Invalid response: " + responseJSON);
            }
            JSONMessage message = new JSONMessage(responseJSON.getJSONObject("message").toString());
            if (!message.has("content")) {
                throw new IOException("Invalid response: " + responseJSON);
            }
            return message;
        } catch (JSONException e) {
            throw new JSONException("Body: " + bodyResponse, e);
        }
    }

    public static class JSONMessage extends JSONObject {
        private JSONMessage(String role, String content) {
            super();

            put("role", role);
            put("content", content);
        }

        private JSONMessage(String json) {
            super(json);
        }

        public String getRole() {
            return optString("role", null);
        }

        public String getContent() {
            return optString("content", null);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new OpenWebUIAPI("https://ai.snam.dev/").chat(new LLMRequest("qwen2.5:3b",
                "What's your favorite color?")).getContent());
    }
}
