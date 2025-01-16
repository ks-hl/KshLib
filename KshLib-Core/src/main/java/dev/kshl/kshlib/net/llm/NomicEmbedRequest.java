package dev.kshl.kshlib.net.llm;

import org.json.JSONObject;

public class NomicEmbedRequest extends EmbedRequest {
    private final Function function;

    public NomicEmbedRequest(String input, Function function) {
        super("nomic-embed-text", input);
        this.function = function;
    }

    @Override
    protected void putContent(JSONObject json) {
        json.put("input", function.toString().toLowerCase() + ": " + getContent());
    }

    /**
     * <a href="https://huggingface.co/nomic-ai/nomic-embed-text-v1">Reference</a>
     */
    public enum Function {
        /**
         * Purpose: embed texts as documents from a dataset
         * This prefix is used for embedding texts as documents, for example as documents for a RAG index.
         */
        SEARCH_DOCUMENT,
        /**
         * Purpose: embed texts as questions to answer
         * This prefix is used for embedding texts as questions that documents from a dataset could resolve, for example as queries to be answered by a RAG application.
         */
        SEARCH_QUERY,
        /**
         * Purpose: embed texts to group them into clusters
         * This prefix is used for embedding texts in order to group them into clusters, discover common topics, or remove semantic duplicates.
         */
        CLUSTERING,
        /**
         * Purpose: embed texts to classify them
         * This prefix is used for embedding texts into vectors that will be used as features for a classification model
         */
        CLASSIFICATION,

    }
}
