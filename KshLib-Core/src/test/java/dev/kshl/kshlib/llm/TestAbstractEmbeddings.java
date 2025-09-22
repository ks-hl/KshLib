package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.Embeddings;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestAbstractEmbeddings {
    private AbstractEmbeddings embeddings123456;

    @BeforeEach
    public void setUp() {
        List<Float> testData123456 = List.of(1f, 2f, 3f, 4f, 5f, 6f);
        embeddings123456 = new FloatEmbeddings(testData123456);
    }

    @Test
    public void testConstructor() {
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f), embeddings123456);
    }

    @Test
    public void testFromJSON() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONArray().put(1.0).put(2.0).put(3.0));
        jsonArray.put(new JSONArray().put(4.0).put(5.0).put(6.0));

        AbstractEmbeddings embeddingsFromJson = Embeddings.fromJSON(jsonArray);
        assertEquals(embeddings123456, embeddingsFromJson);
    }
}
