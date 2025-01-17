package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestLLMEmbeddingsManager {
    @DatabaseTest
    public void testLLMEmbeddings(ConnectionManager connectionManager) throws SQLException, BusyException {
        LLMEmbeddingsManager llmEmbeddingsManager = new LLMEmbeddingsManager(connectionManager, "llm_embeds", LLMEmbeddingsManager.Quant.Q8);

        connectionManager.execute("DROP TABLE IF EXISTS llm_embeds", 3000L);
        connectionManager.execute(llmEmbeddingsManager::init, 3000L);

        AbstractEmbeddings embeds1 = newRandomEmbeds(100, new Random(895453985349L));
        AbstractEmbeddings embeds2 = newRandomEmbeds(100, new Random(569248689349L));

        assertNotEquals(embeds1, embeds2);

        int id1 = llmEmbeddingsManager.put(embeds1);
        int id2 = llmEmbeddingsManager.put(embeds2);

        assertNotEquals(id1, id2);

        assertEquals(id1, llmEmbeddingsManager.put(embeds1));
        assertEquals(id2, llmEmbeddingsManager.put(embeds2));

        assertEquals(id1, llmEmbeddingsManager.query(embeds1, 0.99f).get(0).getKey());
        assertEquals(id2, llmEmbeddingsManager.query(embeds2, 0.99f).get(0).getKey());
    }

    private static AbstractEmbeddings newRandomEmbeds(int length, Random random) {
        List<Float> out = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            out.add(random.nextFloat() * 2 - 1);
        }
        return new FloatEmbeddings(out);
    }
}
