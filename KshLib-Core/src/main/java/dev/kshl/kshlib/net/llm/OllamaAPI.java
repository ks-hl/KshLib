package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.function.ThrowingConsumer;
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

    public static void main(String[] args) throws IOException {
        Map<Embeddings, String> documents = new HashMap<>();
        ThrowingConsumer<String, IOException> consumeDocument = doc -> {
            Embeddings embeddings = embed(doc, NomicEmbedRequest.Function.SEARCH_DOCUMENT).embeddings();

            System.out.println(doc + "\n" + embeddings.getEmbeddings().size() + embeddings.toJSON());
            documents.put(embeddings, doc);
        };

        consumeDocument.accept("My favorite color is purple. That's my favorite color because I like it.");
        consumeDocument.accept("My favorite animal is a giraffe. I like this animal because it has a long neck.");

        ThrowingConsumer<String, IOException> consumeQuery = query -> {
            Embeddings embeddings = embed(query, NomicEmbedRequest.Function.SEARCH_QUERY).embeddings();
            System.out.println("SEARCHING: " + query + "\n" + embeddings.getEmbeddings().size() + embeddings.toJSON());
            for (Map.Entry<Embeddings, String> entry : documents.entrySet()) {
                System.out.println(entry.getKey().compareTo(embeddings) + ": " + entry.getValue());
            }
        };
        consumeQuery.accept("What's your favorite color?");
        consumeQuery.accept("What's your favorite animal?");
        consumeQuery.accept("what is your favourite collourrr??");

        System.out.println("\n\n");

        LLMResponse response = new OllamaAPI("http://10.0.70.105:11434/").generate(new LLMRequest("qwen2.5:3b",
                "Respond only with the corrected text. Correct the spelling in this text: what is your favourite collourrr??"));
        System.out.println(response.getTokensPerSecond() + " t/s");
        System.out.println(response.created_at().toInstant().toEpochMilli());
        System.out.println(System.currentTimeMillis());
        System.out.println(response.created_at().withZoneSameInstant(ZoneId.systemDefault()).format(TimeUtil.ENTRY_TIME_FORMAT));
        System.out.println(response.response());
    }

    private static EmbedResponse embed(String str, NomicEmbedRequest.Function function) throws IOException {
        return new OllamaAPI("http://10.0.70.105:11434/").embeddings(new NomicEmbedRequest(str, function));
    }
}
