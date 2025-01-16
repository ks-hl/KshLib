package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.BasicProfiler;
import dev.kshl.kshlib.misc.FileUtil;
import dev.kshl.kshlib.misc.Formatter;
import dev.kshl.kshlib.misc.Timer;
import dev.kshl.kshlib.sql.ConnectionManager;
import dev.kshl.kshlib.sql.DatabaseTest;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestEmbeddings {
    private List<Float> testData;
    private Embeddings embeddings;

    @BeforeEach
    public void setUp() {
        testData = Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        embeddings = new Embeddings(testData);
    }

    @Test
    public void testConstructor() {
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f), embeddings.getEmbeddings());
    }

    @Test
    public void testFromJSON() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONArray().put(1.0).put(2.0).put(3.0));
        jsonArray.put(new JSONArray().put(4.0).put(5.0).put(6.0));

        Embeddings embeddingsFromJson = new Embeddings(jsonArray);
        assertEquals(embeddings, embeddingsFromJson);
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

        JSONArray result = embeddings.toJSON();
        assertEquals(expectedJson.toString(), result.toString());
    }

    @Test
    public void testGetBytes() {
        byte[] bytes = embeddings.getBytes();
        assertEquals(6 * 4, bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        for (Float value : Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f)) {
            assertEquals(value, buffer.getFloat(), 0.0001);
        }

        assertEquals(embeddings, new Embeddings(bytes));
    }

    @Test
    public void testEquals() {
        Embeddings sameEmbeddings = new Embeddings(testData);
        assertEquals(embeddings, sameEmbeddings);

        List<Float> differentData = List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        Embeddings differentEmbeddings = new Embeddings(differentData);
        assertNotEquals(embeddings, differentEmbeddings);
    }

    @Test
    public void testHashCode() {
        Embeddings sameEmbeddings = new Embeddings(testData);
        assertEquals(embeddings.hashCode(), sameEmbeddings.hashCode());
    }

    @Test
    public void testCompareTo() {
        Embeddings otherEmbeddings = new Embeddings(List.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f));

        assertEquals(1.0, embeddings.compareTo(otherEmbeddings), 0.0001);

        Embeddings differentEmbeddings = new Embeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f));

        double expectedSimilarity = (7 + 2 * 8 + 3 * 9 + 4 * 10 + 5 * 11 + 6 * 12) /
                Math.sqrt((1 + 2 * 2 + 3 * 3 + 4 * 4 + 5 * 5 + 6 * 6) * (7 * 7 + 8 * 8 + 9 * 9 + 10 * 10 + 11 * 11 + 12 * 12));

        assertEquals(expectedSimilarity, embeddings.compareTo(differentEmbeddings), 0.0001);
    }

    @Test
    public void testCompareToDifferentDimensions() {
        Embeddings differentDimensionEmbeddings = new Embeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13f));
        assertThrows(IllegalArgumentException.class, () -> embeddings.compareTo(differentDimensionEmbeddings));
    }

    @Test
    public void testCompareToEmpty() {
        Embeddings emptyEmbeddings = new Embeddings(List.of());
        assertThrows(IllegalArgumentException.class, () -> embeddings.compareTo(emptyEmbeddings));
    }

    @Test
    public void testCompareToZeroNorm() {
        Embeddings zeroNormEmbeddings = new Embeddings(List.of(0f, 0f, 0f, 0f, 0f, 0f));
        assertThrows(IllegalArgumentException.class, () -> embeddings.compareTo(zeroNormEmbeddings));
    }

    @DatabaseTest
    public void testVectorStorage(ConnectionManager connectionManager) throws SQLException, BusyException {
        if (connectionManager.isMySQL()) return;

        connectionManager.execute("""
                CREATE TABLE embeddings (
                    id INTEGER PRIMARY KEY,
                    v768 BLOB NOT NULL,
                    v32 BLOB NOT NULL
                )""", 3000L);

        Random random = new Random(3248975927598L);
        int count = 1000;
        int length = 768;

        this.embeddings = newRandomEmbeddings(length, random);

        connectionManager.execute("INSERT INTO embeddings (v768,v32) VALUES (?,?)", 3000L, embeddings.getBytes(), embeddings.applyPCA(32).getBytes());

        Map<Embeddings, Embeddings> testEmbeddings = loadPCAEmbeddings();
        Iterator<Map.Entry<Embeddings, Embeddings>> it = testEmbeddings.entrySet().iterator();

        while (it.hasNext()) {
            connectionManager.execute(connection -> {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO embeddings (v768,v32) VALUES " + ("(?,?),".repeat((count / 10) - 1)) + "(?,?)")) {
                    for (int i1 = 0; i1 < count / 10 && it.hasNext(); i1++) {
                        Map.Entry<Embeddings, Embeddings> embed = it.next();
                        int index = i1 * 2 + 1;
                        connectionManager.setBlob(connection, preparedStatement, index, embed.getKey().getBytes());
                        connectionManager.setBlob(connection, preparedStatement, index + 1, embed.getValue().getBytes());
                    }
                    preparedStatement.execute();
                }
            }, 3000L);
        }


        Timer timer = new Timer();
        BasicProfiler profiler = new BasicProfiler(1000);
        profiler.start();

        Embeddings highest = connectionManager.query("""
                 SELECT id,v768
                 FROM embeddings
                """, rs -> {
            Embeddings best = null;
            double bestSimilarity = Double.MIN_VALUE;
            while (rs.next()) {
                Embeddings result = new Embeddings(connectionManager.getBlob(rs, 2));
                double similarity = result.compareTo(embeddings);
                if (similarity > bestSimilarity) {
                    best = result;
                    bestSimilarity = similarity;
                }
            }
            return best;
        }, 3000L);
        assertEquals(embeddings, highest);
        System.out.println(timer);
//        System.out.println(profiler.toString(0.01));


        timer = new Timer();

        connectionManager.query("""
                 SELECT id,v32
                 FROM embeddings
                """, rs -> {
            while (rs.next()) {
                Embeddings result = new Embeddings(connectionManager.getBlob(rs, 2));
            }
        }, 3000L);
        assertEquals(embeddings, highest);
        System.out.println(timer);
    }

    private static Map<Embeddings, Embeddings> loadPCAEmbeddings() {
        File file = new File("src/test/resources/embeddings.csv");
        Map<Embeddings, Embeddings> out = new HashMap<>();
        if (file.exists()) {
            String[] parts = FileUtil.read(file).split("[\n\r;]+");
            for (int i = 0; i < parts.length; i += 2) {
                String embedding768 = parts[i];
                String embedding32 = parts[i + 1];

                out.put(Embeddings.fromBase64(embedding768), Embeddings.fromBase64(embedding32));
            }
        } else {
            Random random = new Random(982654989);
            final StringBuilder outString = new StringBuilder();
            int count = 10000;
            int threadCount = 10;
            List<Thread> threads = new ArrayList<>();
            for (int threadN_ = 0; threadN_ < threadCount; threadN_++) {
                final int threadN = threadN_;
                Thread thread = new Thread(() -> {
                    for (int i = 0; i < count / threadCount; i++) {
                        Embeddings embedding768 = newRandomEmbeddings(384, random);
                        Embeddings embedding32 = embedding768.applyPCA(32);
                        synchronized (outString) {
                            outString.append(embedding768.toBase64()).append(";");
                            outString.append(embedding32.toBase64()).append("\n");
                        }

                        if (threadN == 0) {
                            System.out.println("Generating test embeddings: " + Formatter.toString(i * 100 * threadCount / (double) count, 2, true, true) + "%");
                        }
                    }
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                FileUtil.write(file, outString.toString().replace(" ", ""));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return out;
    }

    private static Embeddings newRandomEmbeddings(int length, Random random) {
        List<Float> embeddings = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            embeddings.add(random.nextFloat());
        }
        return new Embeddings(embeddings);
    }

    @Test
    public void testPCA() {
        Embeddings base = newRandomEmbeddings(10, new Random(2));
        Embeddings modified = modifySlightly(base, new Random(4958252L));
        System.out.println(base);
        System.out.println(modified);
        System.out.println(base.applyPCA(5));
        System.out.println(modified.applyPCA(5));
    }

    private static Embeddings modifySlightly(Embeddings base, Random random) {
        List<Float> embeddings = new ArrayList<>();
        for (Float embedding : base.getEmbeddings()) {
            embeddings.add(embedding * random.nextFloat(0.999f, 1.001f));
        }
        return new Embeddings(embeddings);
    }
}
