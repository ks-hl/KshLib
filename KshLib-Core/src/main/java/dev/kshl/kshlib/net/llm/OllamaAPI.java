package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.misc.TimeUtil;
import dev.kshl.kshlib.net.HTTPRequestType;
import dev.kshl.kshlib.net.NetUtil;
import dev.kshl.kshlib.net.NetUtilInterval;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.ZoneId;

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

    public static void main(String[] args) throws IOException {
        embed("What's your favorite color?");
        embed("What is your favorite colorrr??");
        embed("What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?");


        LLMResponse response = new OllamaAPI("http://10.0.70.105:11434/").generate(new LLMRequest("qwen2.5:3b",
                "What's your favorite color?"));
        System.out.println(response.getTokensPerSecond() + " t/s");
        System.out.println(response.created_at().toInstant().toEpochMilli());
        System.out.println(System.currentTimeMillis());
        System.out.println(response.created_at().withZoneSameInstant(ZoneId.systemDefault()).format(TimeUtil.ENTRY_TIME_FORMAT));
        System.out.println(response.response());
    }

    private static void embed(String str) throws IOException {
        EmbedResponse embedResponse = new OllamaAPI("http://10.0.70.105:11434/").embeddings(new EmbedRequest("nomic-embed-text", str));

        System.out.println(embedResponse.getTokensPerSecond() + "t/s: " + embedResponse.embeddings());
    }
}
