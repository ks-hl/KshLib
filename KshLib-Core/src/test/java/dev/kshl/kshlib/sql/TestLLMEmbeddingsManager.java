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
        LLMEmbeddingsManager llmEmbeddingsManager = new LLMEmbeddingsManager(connectionManager, "llm_embeds");

        connectionManager.execute("DROP TABLE IF EXISTS llm_embeds", 3000L);
        connectionManager.execute(llmEmbeddingsManager::init, 3000L);

        AbstractEmbeddings embeds1 = newRandomEmbeds(100, new Random(895453985349L));
        AbstractEmbeddings embeds2 = newRandomEmbeds(100, new Random(569248689349L));

        assertNotEquals(embeds1, embeds2);
    }

    private static AbstractEmbeddings newRandomEmbeds(int length, Random random) {
        List<Float> out = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            out.add(random.nextFloat() * 2 - 1);
        }
        return new FloatEmbeddings(out);
    }
}
