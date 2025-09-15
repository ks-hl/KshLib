package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.llm.EmbeddingsManager;
import dev.kshl.kshlib.llm.LLMRequest;
import dev.kshl.kshlib.llm.OllamaAPI;
import dev.kshl.kshlib.log.StdOutLogger;
import dev.kshl.kshlib.misc.FileUtil;
import dev.kshl.kshlib.misc.Formatter;
import dev.kshl.kshlib.misc.Pair;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class TestEmbeddingsDAO {
    private static final String query = "How do I view my chunk borders of my claim";
    private static final String model = "qwen3:4b-instruct";
    private static final int contextWidth = 1024 * 8;
    private static final OllamaAPI ollamaAPI = new OllamaAPI("http://10.0.70.105:11434");

    private ConnectionManager connectionManager;
    private EmbeddingsDAO embeddingsDAO;
    private EmbeddingsManager embeddingsManager;

    @BeforeEach
    public void init() throws SQLException, IOException, ClassNotFoundException, BusyException {
        connectionManager = new TestSQLManager("10.0.70.110:3306", "test", "test", "password", 5);
        connectionManager.init();
        embeddingsDAO = new EmbeddingsDAO(connectionManager, "embedding");
        connectionManager.execute(embeddingsDAO::init, 3000L);
        embeddingsManager = new EmbeddingsManager("nomic-embed-text:v1.5", 2048, ollamaAPI, embeddingsDAO, new StdOutLogger(), 500);
    }

    @Test
    public void impl() throws SQLException, BusyException, IOException {
        List<File> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(new File("content").toPath())) {
            paths.forEach(path -> {
                File file = path.toFile();
                if (!file.isFile()) return;
                String content = FileUtil.read(file);
                if (content == null || content.trim().toLowerCase().matches("#redirect:?\\s*\\[\\[[^]]+]][\\n\\r]*(\\{\\{[^}]+}})?") || content.length() < 80) {
                    file.delete();
                    return;
                }
                files.add(file);
            });
        }
        files.sort(Comparator.comparing(File::getAbsolutePath));
        System.out.println("Embedding files...");
        embeddingsManager.setEmbedFiles(files);

        System.out.println("Expanding query...");

        JSONObject expanded = expandQuery(query);
        String topic = expanded.getString("topic");
        String correctedQuery = expanded.getString("corrected_query");
        boolean isPolicy = expanded.getBoolean("is_policy");
        System.out.println("Original Query: " + query);
        System.out.println("Corrected Query: " + correctedQuery);
        System.out.println("is_policy: " + isPolicy);
        System.out.println("topic: " + topic);
        rag(correctedQuery, topic, isPolicy);
        if (isPolicy) {
            System.out.println("I'm an AI, I make mistakes. Please check /rules and /policies, or ask a staff member.");
        }
    }

    private JSONObject expandQuery(String query) throws IOException {
        for (int i = 0; i < 3; i++) { // Give 3 attempts at valid JSON
            String systemPrompt = """
                    You are given a user query from a Minecraft server player. You will correct the syntax of that query and provide additional information.
                    
                    Your output must always be valid JSON with all of these keys, and nothing else:
                    1. "corrected_query": The user query, but with any spelling, capitalization, and grammatical errors fixed.
                    2. "is_policy": A boolean flag. Set this to true if the user query is about server rules, moderation, bans, allowed/disallowed behavior, or any other policy-related question. If asking "Am I allowed to.." "Can I...", set it to true. Otherwise, set it to false.
                    3. "topic": The topic of the query according to the next section.
                    
                    If the query pertains to the following, respond with the corresponding topic in the "topic" section:
                    - Whether a certain client mod is allowed → rules
                    - Whether a player can take from a chest or area, or questions about the abandoned policy → rules
                    - AFK farming and chunk-loaders → rules
                    - How to do something with Towny → towny
                    - How to brew something such as drinks or other alcohol → brewery
                    - AFK farming and chunk-loaders → rules
                    - Other questions about commands → info
                    - Topic not listed → {leave empty}
                    """;
            var response = ollamaAPI.generate(new LLMRequest(model, query)
                    .system(systemPrompt)
                    .contextLength(contextWidth)
                    .think(false)
            );
            System.out.println(String.format("t/s: %s/%s", Formatter.toString(response.getPromptTokensPerSecond(), 1, true, true), Formatter.toString(response.getEvalTokensPerSecond(), 1, true, true)));
            try {
                String responseText = response.response().trim();
                if (responseText.endsWith("```")) {
                    responseText = responseText.substring(0, responseText.length() - 3);
                }
                if (responseText.contains("{")) {
                    responseText = responseText.substring(responseText.indexOf("{"));
                }
                JSONObject jsonObject = new JSONObject(responseText);
                jsonObject.getString("corrected_query");
                jsonObject.getBoolean("is_policy");
                jsonObject.getString("topic");
                return jsonObject;
            } catch (JSONException e) {
                System.err.println("Invalid response: " + response);
                e.printStackTrace();
            }
        }
        throw new IllegalStateException("Failed to obtain valid JSON response after 3 attempts.");
    }

    private String rag(String query, String topic, boolean isPolicy) throws SQLException, BusyException, IOException {
        float[] embed = embeddingsManager.getEmbeddingsFloat(query);

        List<String> context = new ArrayList<>();

        String prompt = "USER QUERY:\n\n%s\n\nCONTEXT:%s";
        embeddingsDAO.getNeighbors(embed, 40, topic, 0.6f).stream()
//                .filter(n -> !isPolicy || n.getLeft().contains("content/rules/"))
                .limit(12).map(Pair::getLeft).forEach(line -> {
                    int index = line.indexOf("\n\n");
                    String file = line.substring(0, index);
                    line = line.substring(index + 2);
                    context.add(String.format("<%s>%s</%s>", file, line, file));
                });
        String systemPrompt = """
                You are a helpful chat assistant that answers only using the provided context. If the answer is not in the context, say "I don't know that, try asking it a different way :)"; do not guess.
                - Use only the CONTEXT to answer the USER QUERY.
                - Some CONTEXT may be irrelevant and should be ignored.
                - Keep responses user-friendly, do not directly mention the existence of "context" or "files" that you are referencing.
                - If multiple chunks conflict, prefer the earliest.
                - Only answer the USER QUERY; do not provide any extra information that is not relevant to the USER QUERY.
                - Use plain text, ≤100 words.
                - Keep responses to one line and casual.
                - Wrap any slash-commands in <blue></blue> e.g <blue>/rules</blue> or <blue>/info</blue>
                """;

        System.out.println("   " + String.format(prompt, query, context.stream().map(s -> s.replaceAll("[\n\r]+", "\\\\n")).reduce((a, b) -> a + "\n" + b).orElse("")).replaceAll("[\n\r]+", "\n   "));
        var response = ollamaAPI.generate(new LLMRequest(model, String.format(prompt, query, context))
                .system(systemPrompt)
                .contextLength(contextWidth)
                .think(false)
        );
        System.out.println("Prompt tokens:" + response.prompt_eval_count());
        System.out.println(String.format("t/s: %s/%s", Formatter.toString(response.getPromptTokensPerSecond(), 1, true, true), Formatter.toString(response.getEvalTokensPerSecond(), 1, true, true)));
        System.out.println("RESPONSE: " + response.response() + "\nEND RESPONSE");

        return response.response();
    }

    private static class TestSQLManager extends ConnectionManager {
        public TestSQLManager(String hostAndPort, String database, String user, String password, int poolSize) throws IOException, SQLException, ClassNotFoundException {
            super(hostAndPort, database, user, password, poolSize);
        }

        @Override
        protected void init(Connection connection) throws SQLException {
        }

        @Override
        protected void debug(String line) {
            System.out.println(line);
        }

        @Override
        protected boolean checkAsync() {
            return true;
        }

        @Override
        protected boolean isDebug() {
            return false;
        }
    }
}
