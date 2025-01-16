package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.net.HTTPRequestType;
import dev.kshl.kshlib.net.NetUtil;
import dev.kshl.kshlib.net.NetUtilInterval;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class OllamaAPI extends NetUtilInterval {

    public OllamaAPI(String url) {
        super(url, 50);
    }

    public LLMResponse generate(LLMRequest llmRequest) throws IOException {
        String bodyResponse = null;
        try {
            NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit("api/generate"), HTTPRequestType.POST, false);
            request.body(llmRequest.toJSON().toString());
            request.headers("content-type", "application/json");
            request.timeout(llmRequest.getTimeout());
            var response = request.request();
            if (response.getResponseCode().isError()) {
                throw new IOException(response.getResponseCode() + " (" + response.getResponseCode().getCode() + "): " + response.getBody());
            }
            bodyResponse = response.getBody();
            JSONObject responseJSON = response.getJSON();

            try {
                return LLMResponse.fromJSON(responseJSON);
            } catch (JSONException e) {
                throw new IOException("Invalid response: " + responseJSON, e);
            }
        } catch (JSONException e) {
            throw new JSONException("Body: " + bodyResponse, e);
        }
    }

    public EmbedResponse embeddings(EmbedRequest embedRequest) throws IOException {
        NetUtil.Request request = new NetUtil.Request(adaptSuffixAndRateLimit("api/embed"), HTTPRequestType.POST, false);
        request.body(embedRequest.toJSON().toString());
        request.headers("content-type", "application/json");
        request.timeout(embedRequest.getTimeout());
        var response = request.request();
        if (response.getResponseCode().isError()) {
            throw new IOException(response.getResponseCode() + " (" + response.getResponseCode().getCode() + "): " + response.getBody());
        }
        String bodyResponse = response.getBody();
        JSONObject responseJSON;
        try {
            responseJSON = response.getJSON();
        } catch (JSONException e) {
            throw new JSONException("Response not JSON. Body: " + bodyResponse, e);
        }
        try {
            return EmbedResponse.fromJSON(responseJSON);
        } catch (JSONException e) {
            throw new IOException("Invalid response: " + responseJSON, e);
        }
    }

//    public static void main(String[] args) throws IOException {
//        LLMResponse response = new OllamaAPI("http://10.0.70.105:11434/").embeddings(new LLMRequest("nomic-embed-text",
//                "What's your favorite color?"));
//        System.out.println(response.getTokensPerSecond() + " t/s");
//        System.out.println(response.created_at().toInstant().toEpochMilli());
//        System.out.println(System.currentTimeMillis());
//        System.out.println(response.created_at().withZoneSameInstant(ZoneId.systemDefault()).format(TimeUtil.ENTRY_TIME_FORMAT));
//        System.out.println(response);
//    }

    public static void main(String[] args) throws IOException {
        embed("What's your favorite color?");
        embed("What is your favorite colorrr??");
        embed("What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?What's your favorite color?");
    }

    private static void embed(String str) throws IOException {
        EmbedResponse embedResponse = new OllamaAPI("http://10.0.70.105:11434/").embeddings(new EmbedRequest("nomic-embed-text", str));

        System.out.println(embedResponse.getTokensPerSecond() + "t/s: " + embedResponse.embeddings());
    }
}
