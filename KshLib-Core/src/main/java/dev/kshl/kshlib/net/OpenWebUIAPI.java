package dev.kshl.kshlib.net;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;

public class OpenWebUIAPI extends NetUtilInterval {
    /*
    curl 'https://ai.snam.dev/ollama/api/chat' \
  -H 'accept: application/json' \
  -H 'accept-language: en-US,en;q=0.9' \
  -H 'origin: https://ai.snam.dev' \
  -H 'priority: u=1, i' \
  -H 'referer: https://ai.snam.dev/?temporary-chat=true' \
  -H 'sec-ch-ua: "Chromium";v="131", "Not_A Brand";v="24"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "Linux"' \
  -H 'sec-fetch-dest: empty' \
  -H 'sec-fetch-mode: cors' \
  -H 'sec-fetch-site: same-origin' \
  -H 'user-agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36' \
  --data-raw '{"stream":false,"model":"qwen2.5:72b-instruct-q4_K_S",,"options":{},"chat_id":"local"}'
     */

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
            json.put("model", llmRequest.model);
            json.put("messages", new JSONArray().put(new JSONMessage("user", llmRequest.content)));
            json.put("options", new JSONObject()
                    .put("seed", llmRequest.seed)
                    .put("num_ctx", llmRequest.contextLength)
            );
            NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit("ollama/api/chat"), HTTPRequestType.POST, false);
            request.body(json.toString());
            request.headers(
                    "authorization", "Bearer " + key,
                    "cookie", "token=" + key,
                    "content-type", "application/json"
            );
            request.timeout(llmRequest.timeout);
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

    public static class LLMRequest {

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
