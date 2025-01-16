package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.Embeddings;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestAbstractEmbeddings {
    private List<Float> testData123456;
    private AbstractEmbeddings embeddings123456;

    @BeforeEach
    public void setUp() {
        testData123456 = List.of(1f, 2f, 3f, 4f, 5f, 6f);
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

    @Test
    public void testToJSON() {
        JSONArray expectedJson = new JSONArray();
        expectedJson.put(new BigDecimal("1.0"));
        expectedJson.put(new BigDecimal("2.0"));
        expectedJson.put(new BigDecimal("3.0"));
        expectedJson.put(new BigDecimal("4.0"));
        expectedJson.put(new BigDecimal("5.0"));
        expectedJson.put(new BigDecimal("6.0"));

        JSONArray result = embeddings123456.toJSON();
        assertEquals(expectedJson.toString(), result.toString());
    }

    @Test
    public void testGetBytes() {
        byte[] bytes = embeddings123456.getBytes();
        assertEquals(6 * 4, bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        for (Float value : testData123456) {
            assertEquals(value, buffer.getFloat(), 0.0001);
        }

        assertEquals(embeddings123456, Embeddings.fromBytes(bytes));
    }

    @Test
    public void testEquals() {
        AbstractEmbeddings sameEmbeddings = new FloatEmbeddings(testData123456);
        assertEquals(embeddings123456, sameEmbeddings);

        List<Float> differentData = List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        AbstractEmbeddings differentEmbeddings = new FloatEmbeddings(differentData);
        assertNotEquals(embeddings123456, differentEmbeddings);
    }

    @Test
    public void testHashCode() {
        assertEquals(testData123456.hashCode(), embeddings123456.hashCode());
    }

    @Test
    public void testCompareCosine() {
        AbstractEmbeddings otherEmbeddings = new FloatEmbeddings(testData123456);

        assertEquals(1.0, embeddings123456.compareCosine(otherEmbeddings), 0.0001);

        AbstractEmbeddings differentEmbeddings = new FloatEmbeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f));

        double expectedSimilarity = (7 + 2 * 8 + 3 * 9 + 4 * 10 + 5 * 11 + 6 * 12) /
                Math.sqrt((1 + 2 * 2 + 3 * 3 + 4 * 4 + 5 * 5 + 6 * 6) * (7 * 7 + 8 * 8 + 9 * 9 + 10 * 10 + 11 * 11 + 12 * 12));

        assertEquals(expectedSimilarity, embeddings123456.compareCosine(differentEmbeddings), 0.0001);
    }

    @Test
    public void testCompareCosineDifferentDimensions() {
        AbstractEmbeddings differentDimensionEmbeddings = new FloatEmbeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(differentDimensionEmbeddings));
    }

    @Test
    public void testCompareCosineEmpty() {
        AbstractEmbeddings emptyEmbeddings = new FloatEmbeddings(List.of());
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(emptyEmbeddings));
    }

    @Test
    public void testCompareCosineZeroNorm() {
        AbstractEmbeddings zeroNormEmbeddings = new FloatEmbeddings(List.of(0f, 0f, 0f, 0f, 0f, 0f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(zeroNormEmbeddings));
    }

    private static AbstractEmbeddings newRandomEmbeddings(int length, Random random) {
        List<Float> embeddings = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            embeddings.add(random.nextFloat() * 2 - 1);
        }
        return new FloatEmbeddings(embeddings);
    }
}
