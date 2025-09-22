package dev.kshl.kshlib.llm;

public class GemmaEmbedRequest extends EmbedRequest {
    private GemmaEmbedRequest(String input) {
        super("embeddinggemma", input);
    }

    public static GemmaEmbedRequest document(String title, String content) {
        return new GemmaEmbedRequest(String.format("title: %s | text: %s", title, content));
    }

    public static GemmaEmbedRequest query(String query) {
        return new GemmaEmbedRequest(String.format("task: search result | query: %s", query));
    }
}
