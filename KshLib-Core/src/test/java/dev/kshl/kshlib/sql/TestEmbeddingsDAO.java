package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.llm.EmbeddingsManager;
import dev.kshl.kshlib.llm.LLMRequest;
import dev.kshl.kshlib.llm.OllamaAPI;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;
import dev.kshl.kshlib.misc.FileUtil;
import dev.kshl.kshlib.misc.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class TestEmbeddingsDAO {
    @DatabaseTest
    public void testLLMEmbeddings(ConnectionManager connectionManager) throws SQLException, BusyException, IOException {
//        EmbeddingsDAO embeddingsDAO = new EmbeddingsDAO(connectionManager, "llm_embeds");
//
//        connectionManager.execute("DROP TABLE IF EXISTS llm_embeds", 3000L);
//        connectionManager.execute(embeddingsDAO::init, 3000L);
//
//        AbstractEmbeddings embeds1 = newRandomEmbeds(100, new Random(895453985349L));
//        AbstractEmbeddings embeds2 = newRandomEmbeds(100, new Random(569248689349L));
//
//        assertNotEquals(embeds1, embeds2);
    }

    private static AbstractEmbeddings newRandomEmbeds(int length, Random random) {
        List<Float> out = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            out.add(random.nextFloat() * 2 - 1);
        }
        return new FloatEmbeddings(out);
    }

    private final OllamaAPI ollamaAPI = new OllamaAPI("http://10.0.70.105:11434");
    private ConnectionManager connectionManager;
    private EmbeddingsDAO embeddingsDAO;
    private EmbeddingsManager embeddingsManager;

    @BeforeEach
    public void init() throws SQLException, IOException, ClassNotFoundException, BusyException {
        connectionManager = new TestSQLManager("10.0.70.110:3306", "test", "test", "password", 5);
        connectionManager.init();
        embeddingsDAO = new EmbeddingsDAO(connectionManager, "embedding");
        connectionManager.execute(embeddingsDAO::init, 3000L);
        embeddingsManager = new EmbeddingsManager("nomic-embed-text:v1.5", 2048, ollamaAPI, embeddingsDAO, System.err::println, 500);
    }

    @Test
    public void print() throws SQLException, BusyException {
        connectionManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, value, file_hash, LENGTH(file_hash) FROM embedding_paths ORDER BY id")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(
                                rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getInt(4));
                    }
                }
            }
        }, 3000L);
        connectionManager.execute("CREATE TABLE IF NOT EXISTS ping (ping INT)", 3000L);
    }

    @Test
    public void impl() throws SQLException, BusyException, IOException {
        List<File> files = new ArrayList<>();
        FileUtil.walkFileTree(new File("content"), 10, file -> {
            if (file.isFile()) files.add(file);
        });
        files.sort(Comparator.comparing(File::getAbsolutePath));
        String query = "Am I allowed to use fullbright?";
        if (query.toLowerCase().contains("allowed") || query.toLowerCase().contains("can i")) {
            query +=" /rules/rule.txt /rules/policies";
        }
//        embeddingsManager.setEmbedFiles(files);
        float[] embed = embeddingsManager.getEmbeddingsFloat(query);

        StringBuilder msg = new StringBuilder();


        msg.append("USER QUERY:\n\n").append(query);
        msg.append("\n\nCONTEXT:");
        embeddingsDAO.getNeighbors(embed, 12).stream().map(Pair::getLeft).forEach(line ->
                msg.append("\n - ").append(line.replaceAll("[\n\r]+", "\\\\n"))
        );
        String systemPrompt = """
                You are a helpful chat assistant that answers only using the provided context. If the answer is not in the context, say "I don't know that, try asking it a different way :)"; do not guess.
                - Use only the CONTEXT to answer the USER QUERY.
                - Note: Some CONTEXT may be irrelevant and should be ignored.
                - If multiple chunks conflict, prefer the earliest.
                - Only answer the USER QUERY; do not provide any extra information that is not relevant to the USER QUERY.
                - Use plain text, ≤100 words.
                - Keep responses to one line, and casual.
                - If answering a question about whether a player is allowed to do something under the rules, suffix your response with "I'm an AI, I make mistakes. Please check /rules and /policies, or ask a staff member."
                """;
        System.out.println(msg);
        var response = ollamaAPI.generate(new LLMRequest("qwen3:8b", msg.toString()).system(systemPrompt).contextLength(8192).think(false));
        System.out.println("RESPONSE: " + response.response() + "\nEND RESPONSE");
        System.out.println("Prompt tokens:" + response.prompt_eval_count());
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
