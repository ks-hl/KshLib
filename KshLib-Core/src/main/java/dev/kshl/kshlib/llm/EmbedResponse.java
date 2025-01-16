package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.Embeddings;
import org.json.JSONObject;

import java.time.Duration;

/**
 * @param model             The model used to generate the response
 * @param load_duration     Time taken to load the model into memory
 * @param prompt_eval_count Input token count
 * @param total_duration    Time to load, ingest, and generate
 * @param embeddings        The embeddings
 */
public record EmbedResponse(
        String model,
        Duration load_duration,
        int prompt_eval_count,
        Duration total_duration,
        AbstractEmbeddings embeddings
) {
    static EmbedResponse fromJSON(JSONObject jsonObject) {
        String model = jsonObject.getString("model");

        Duration load_duration = Duration.ofNanos(jsonObject.getLong("load_duration"));

        int prompt_eval_count = jsonObject.getInt("prompt_eval_count");

        Duration total_duration = Duration.ofNanos(jsonObject.getLong("total_duration"));

        AbstractEmbeddings embeddings = Embeddings.fromJSON(jsonObject.getJSONArray("embeddings"));

        return new EmbedResponse(model, load_duration, prompt_eval_count, total_duration, embeddings);
    }

    public double getTokensPerSecond() {
        return prompt_eval_count() * 1000000000D / total_duration().minus(load_duration()).toNanos();
    }
}