package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.Pair;
import dev.kshl.kshlib.misc.Timer;
import dev.kshl.kshlib.sql.ConnectionManager;
import dev.kshl.kshlib.sql.DatabaseTest;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    private AbstractEmbeddings embeddingsRandom100;

    @BeforeEach
    public void setUp() {
        testData123456 = List.of(1f, 2f, 3f, 4f, 5f, 6f);
        embeddings123456 = new AbstractEmbeddings(testData123456);
        embeddingsRandom100 = newRandomEmbeddings(100, new Random(329439627885943525L));
    }

    @Test
    public void testConstructor() {
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f), embeddings123456.getEmbeddings());
    }

    @Test
    public void testFromJSON() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONArray().put(1.0).put(2.0).put(3.0));
        jsonArray.put(new JSONArray().put(4.0).put(5.0).put(6.0));

        AbstractEmbeddings embeddingsFromJson = new AbstractEmbeddings(jsonArray);
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

        assertEquals(embeddings123456, new AbstractEmbeddings(bytes));
    }

    @Test
    public void testEquals() {
        AbstractEmbeddings sameEmbeddings = new AbstractEmbeddings(testData123456);
        assertEquals(embeddings123456, sameEmbeddings);

        List<Float> differentData = List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        AbstractEmbeddings differentEmbeddings = new AbstractEmbeddings(differentData);
        assertNotEquals(embeddings123456, differentEmbeddings);
    }

    @Test
    public void testHashCode() {
        assertEquals(testData123456.hashCode(), embeddings123456.hashCode());
    }

    @Test
    public void testCompareCosine() {
        AbstractEmbeddings otherEmbeddings = new AbstractEmbeddings(testData123456);

        assertEquals(1.0, embeddings123456.compareCosine(otherEmbeddings), 0.0001);

        AbstractEmbeddings differentEmbeddings = new AbstractEmbeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f));

        double expectedSimilarity = (7 + 2 * 8 + 3 * 9 + 4 * 10 + 5 * 11 + 6 * 12) /
                Math.sqrt((1 + 2 * 2 + 3 * 3 + 4 * 4 + 5 * 5 + 6 * 6) * (7 * 7 + 8 * 8 + 9 * 9 + 10 * 10 + 11 * 11 + 12 * 12));

        assertEquals(expectedSimilarity, embeddings123456.compareCosine(differentEmbeddings), 0.0001);
    }

    @Test
    public void testCompareCosineDifferentDimensions() {
        AbstractEmbeddings differentDimensionEmbeddings = new AbstractEmbeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(differentDimensionEmbeddings));
    }

    @Test
    public void testCompareCosineEmpty() {
        AbstractEmbeddings emptyEmbeddings = new AbstractEmbeddings(List.of());
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(emptyEmbeddings));
    }

    @Test
    public void testCompareCosineZeroNorm() {
        AbstractEmbeddings zeroNormEmbeddings = new AbstractEmbeddings(List.of(0f, 0f, 0f, 0f, 0f, 0f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareCosine(zeroNormEmbeddings));
    }

    @DatabaseTest
    public void testVectorStorage(ConnectionManager connectionManager) throws SQLException, BusyException, IOException, ClassNotFoundException {
        if (connectionManager.isMySQL()) return;

        connectionManager.execute("""
                CREATE TABLE embeddings (
                    id INTEGER PRIMARY KEY,
                    v768 BLOB NOT NULL,
                    x0 INT,
                    x1 INT,
                    x2 INT,
                    x3 INT,
                    x4 INT,
                    x5 INT,
                    x6 INT,
                    x7 INT,
                    x8 INT,
                    x9 INT
                )""", 3000L);

        Random random = new Random(3248975927598L);
        int count = 1000;
        int sourceSize = 768;
        int truncatedSize = 32;

        this.embeddings123456 = newRandomEmbeddings(sourceSize, random);


        String per = "(" + "?,".repeat(10) + "?)";

        List<Integer> sourceIntList = embeddings123456.getFirstN(10).getEmbeddingsIntList();
        connectionManager.execute("INSERT INTO embeddings (v768,x0,x1,x2,x3,x4,x5,x6,x7,x8,x9) VALUES " + per, 3000L,
                embeddings123456.getBytes(),
                sourceIntList.get(0),
                sourceIntList.get(1),
                sourceIntList.get(2),
                sourceIntList.get(3),
                sourceIntList.get(4),
                sourceIntList.get(5),
                sourceIntList.get(6),
                sourceIntList.get(7),
                sourceIntList.get(8),
                sourceIntList.get(9));

        List<Pair<AbstractEmbeddings, AbstractEmbeddings>> testEmbeddings = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            AbstractEmbeddings embeddings = newRandomEmbeddings(sourceSize, random);
            testEmbeddings.add(new Pair<>(embeddings, embeddings.getFirstN(truncatedSize)));
        }

        for (int i = 0; i < testEmbeddings.size(); i++) {
            int start = i;
            int end = Math.min(i + 1000, testEmbeddings.size());
            connectionManager.execute(connection -> {
                String values = (per + ",").repeat(end - start - 1) + per;
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO embeddings (v768,x0,x1,x2,x3,x4,x5,x6,x7,x8,x9) VALUES " + values)) {
                    for (int i1 = 0; start + i1 < end; i1++) {
                        Pair<AbstractEmbeddings, AbstractEmbeddings> embed = testEmbeddings.get(start + i1);
                        int index = i1 * 11 + 1;
                        connectionManager.setBlob(connection, preparedStatement, index, embed.getKey().getBytes());
                        List<Integer> intList = embed.getKey().getFirstN(10).getEmbeddingsIntList();
                        for (int i2 = 0; i2 < intList.size(); i2++) {
                            preparedStatement.setInt(index + i2 + 1, intList.get(i2));
                        }
                    }
                    preparedStatement.execute();
                }
            }, 3000L);
        }


        Timer timer = new Timer();

        AbstractEmbeddings highest = connectionManager.query("""
                 SELECT id,v768
                 FROM embeddings
                """, rs -> {
            AbstractEmbeddings best = null;
            double bestSimilarity = Double.MIN_VALUE;
            while (rs.next()) {
                AbstractEmbeddings result = new AbstractEmbeddings(connectionManager.getBlob(rs, 2));
                double similarity = result.compareCosine(embeddings123456);
                if (similarity > bestSimilarity) {
                    best = result;
                    bestSimilarity = similarity;
                }
            }
            return best;
        }, 3000L);
        assertEquals(embeddings123456, highest);
        System.out.println(timer);


        timer = new Timer();

        String perLookup = "abs(%-?)<" + (int) (Integer.MAX_VALUE * 0.1f);
        String where = (perLookup + " AND ").repeat(10 - 1) + perLookup;
        for (int i = 0; i < 10; i++) {
            where = where.replaceFirst("%", "x" + i);
        }
        highest = connectionManager.query("""
                         SELECT id,v768
                         FROM embeddings
                         WHERE\s
                        """ + where, rs -> {
                    AbstractEmbeddings best = null;
                    double bestSimilarity = Double.MIN_VALUE;
                    int cnt = 0;

                    while (rs.next()) {
                        AbstractEmbeddings result = new AbstractEmbeddings(connectionManager.getBlob(rs, 2));
                        double similarity = result.compareCosine(embeddings123456);
                        if (similarity > bestSimilarity) {
                            best = result;
                            bestSimilarity = similarity;
                        }
                        cnt++;
                    }
                    System.out.println("Iterated " + cnt);
                    return best;
                }, 3000L, sourceIntList.get(0),
                sourceIntList.get(1),
                sourceIntList.get(2),
                sourceIntList.get(3),
                sourceIntList.get(4),
                sourceIntList.get(5),
                sourceIntList.get(6),
                sourceIntList.get(7),
                sourceIntList.get(8),
                sourceIntList.get(9));
        assertEquals(embeddings123456, highest);
        System.out.println(timer);
    }

    private static AbstractEmbeddings newRandomEmbeddings(int length, Random random) {
        List<Float> embeddings = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            embeddings.add(random.nextFloat() * 2 - 1);
        }
        return new AbstractEmbeddings(embeddings);
    }

    private static final Random truncateRandom = new Random(9834769830197523L);

    @RepeatedTest(value = 10000)
    public void testTruncateDistances() {
        AbstractEmbeddings embeddings1 = newRandomEmbeddings(768, truncateRandom);
        AbstractEmbeddings embeddings2;
        double compare;
        do {
            embeddings2 = newRandomEmbeddings(768, truncateRandom);
            compare = embeddings1.compareCosine(embeddings2);
        } while (false);//compare < 0.8);

        AbstractEmbeddings embeddings1Truncated = embeddings1.getFirstN(32);
        AbstractEmbeddings embeddings2Truncated = embeddings2.getFirstN(32);

        assertEquals(compare, embeddings1Truncated.compareCosine(embeddings2Truncated), .3);
    }

    @Test
    public void testOUGRIGERNI() {
        Random random = new Random(45867453875L);
        List<Float> rand = new ArrayList<>();
        int diff = 16;
        for (int i = 0; i < 768 - diff; i++) {
            rand.add(random.nextFloat() * 2 - 1);
        }

        List<Float> rand1 = new ArrayList<>(rand);
        List<Float> rand2 = new ArrayList<>(rand);
        for (int i = 0; i < diff; i++) {
            rand1.add(-1f);
            rand2.add(1f);
        }

        System.out.println(new AbstractEmbeddings(rand1).compareCosine(new AbstractEmbeddings(rand2)));
    }
}
