package dev.kshl.kshlib.llm;

import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @param created_at           The time when the response was created.<br>
 *                             To convert to epoch milliseconds, use {@link ZonedDateTime#toInstant()} then {@link Instant#toEpochMilli()} <br>
 *                             To convert to local timezone, use {@link ZonedDateTime#withZoneSameInstant(ZoneId)} with {@link ZoneId#systemDefault()}
 * @param model                The model used to generate the response
 * @param load_duration        Time taken to load the model into memory
 * @param prompt_eval_count    Input token count
 * @param prompt_eval_duration Time taken to evaluate the prompt
 * @param eval_count           Output token count
 * @param eval_duration        Time taken to generate the output
 * @param total_duration       Time to load, ingest, and generate
 * @param response             The response
 */
public record LLMResponse(
        ZonedDateTime created_at,
        String model,

        // LOAD MODEL
        Duration load_duration,

        // EVALUATE PROMPT
        int prompt_eval_count,
        Duration prompt_eval_duration,

        // GENERATE OUTPUT
        int eval_count,
        Duration eval_duration,

        Duration total_duration,

        String response
) {
    static LLMResponse fromJSON(JSONObject jsonObject) {
        String created_at_str = jsonObject.getString("created_at");

        ZonedDateTime created_at = ZonedDateTime.parse(created_at_str, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String model = jsonObject.getString("model");

        Duration load_duration = Duration.ofNanos(jsonObject.getLong("load_duration"));

        int prompt_eval_count = jsonObject.getInt("prompt_eval_count");
        Duration prompt_eval_duration = Duration.ofNanos(jsonObject.getLong("prompt_eval_duration"));

        int eval_count = jsonObject.getInt("eval_count");
        Duration eval_duration = Duration.ofNanos(jsonObject.getLong("eval_duration"));

        Duration total_duration = Duration.ofNanos(jsonObject.getLong("total_duration"));

        String response = jsonObject.getString("response");

        return new LLMResponse(created_at, model, load_duration, prompt_eval_count, prompt_eval_duration, eval_count, eval_duration, total_duration, response);
    }

    public double getPromptTokensPerSecond() {
        return prompt_eval_count() * 1E9D / prompt_eval_duration().toNanos();
    }

    public double getEvalTokensPerSecond() {
        return eval_count() * 1E9D / eval_duration().toNanos();
    }
}