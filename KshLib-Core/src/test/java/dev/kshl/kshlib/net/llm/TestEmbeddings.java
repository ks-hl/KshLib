package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ThrowingConsumer;
import dev.kshl.kshlib.misc.BasicProfiler;
import dev.kshl.kshlib.misc.Formatter;
import dev.kshl.kshlib.misc.Timer;
import dev.kshl.kshlib.sql.ConnectionFunction;
import dev.kshl.kshlib.sql.ConnectionManager;
import dev.kshl.kshlib.sql.DatabaseTest;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Connection;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEmbeddings {
    private List<Float> testData123456;
    private Embeddings embeddings123456;
    private Embeddings embeddingsRandom100;

    @BeforeEach
    public void setUp() {
        testData123456 = List.of(1f, 2f, 3f, 4f, 5f, 6f);
        embeddings123456 = new Embeddings(testData123456);
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

        Embeddings embeddingsFromJson = new Embeddings(jsonArray);
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

        assertEquals(embeddings123456, new Embeddings(bytes));
    }

    @Test
    public void testEquals() {
        Embeddings sameEmbeddings = new Embeddings(testData123456);
        assertEquals(embeddings123456, sameEmbeddings);

        List<Float> differentData = List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f);
        Embeddings differentEmbeddings = new Embeddings(differentData);
        assertNotEquals(embeddings123456, differentEmbeddings);
    }

    @Test
    public void testHashCode() {
        assertEquals(testData123456.hashCode(), embeddings123456.hashCode());
    }

    @Test
    public void testCompareTo() {
        Embeddings otherEmbeddings = new Embeddings(testData123456);

        assertEquals(1.0, embeddings123456.compareTo(otherEmbeddings), 0.0001);

        Embeddings differentEmbeddings = new Embeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f));

        double expectedSimilarity = (7 + 2 * 8 + 3 * 9 + 4 * 10 + 5 * 11 + 6 * 12) /
                Math.sqrt((1 + 2 * 2 + 3 * 3 + 4 * 4 + 5 * 5 + 6 * 6) * (7 * 7 + 8 * 8 + 9 * 9 + 10 * 10 + 11 * 11 + 12 * 12));

        assertEquals(expectedSimilarity, embeddings123456.compareTo(differentEmbeddings), 0.0001);
    }

    @Test
    public void testCompareToDifferentDimensions() {
        Embeddings differentDimensionEmbeddings = new Embeddings(List.of(7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareTo(differentDimensionEmbeddings));
    }

    @Test
    public void testCompareToEmpty() {
        Embeddings emptyEmbeddings = new Embeddings(List.of());
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareTo(emptyEmbeddings));
    }

    @Test
    public void testCompareToZeroNorm() {
        Embeddings zeroNormEmbeddings = new Embeddings(List.of(0f, 0f, 0f, 0f, 0f, 0f));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.compareTo(zeroNormEmbeddings));
    }

    @DatabaseTest
    public void testVectorStorage(ConnectionManager connectionManager) throws SQLException, BusyException, IOException, ClassNotFoundException {
        if (connectionManager.isMySQL()) return;

        connectionManager.execute("""
                CREATE TABLE embeddings (
                    id INTEGER PRIMARY KEY,
                    v768 BLOB NOT NULL,
                    v32 BLOB NOT NULL
                )""", 3000L);

        Random random = new Random(3248975927598L);
        int count = 1000;
        int length = 384;

        this.embeddings123456 = newRandomEmbeddings(length, random);

        connectionManager.execute("INSERT INTO embeddings (v768,v32) VALUES (?,?)", 3000L, embeddings123456.getBytes(), embeddings123456.applyPCA(32).getBytes());

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
                double similarity = result.compareTo(embeddings123456);
                if (similarity > bestSimilarity) {
                    best = result;
                    bestSimilarity = similarity;
                }
            }
            return best;
        }, 3000L);
        assertEquals(embeddings123456, highest);
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
        assertEquals(embeddings123456, highest);
        System.out.println(timer);
    }

    private static Map<Embeddings, Embeddings> loadPCAEmbeddings() throws SQLException, IOException, ClassNotFoundException, BusyException {
        File sqliteFile = new File("src/test/resources/embeddings.db");
        try (ConnectionManager connectionManager = new ConnectionManager(sqliteFile) {
            @Override
            protected void init(Connection connection) throws SQLException {
                execute(connection, """
                        CREATE TABLE IF NOT EXISTS embeddings (
                            id INTEGER PRIMARY KEY,
                            v768 BLOB NOT NULL,
                            v32 BLOB NOT NULL
                        )""");
            }

            @Override
            protected void debug(String line) {

            }

            @Override
            protected boolean checkAsync() {
                return true;
            }

            @Override
            protected boolean isDebug() {
                return false;
            }
        }) {
            connectionManager.init();
            boolean exists = connectionManager.execute((ConnectionFunction<Boolean>) connection -> connectionManager.count(connection, "embeddings") > 0, 3000L);

            Map<Embeddings, Embeddings> out = new HashMap<>();
            if (exists) {
                connectionManager.query("""
                         SELECT v768,v32
                         FROM embeddings
                        """, rs -> {
                    while (rs.next()) {
                        Embeddings v768 = new Embeddings(connectionManager.getBlob(rs, 1));
                        Embeddings v32 = new Embeddings(connectionManager.getBlob(rs, 2));

                        out.put(v768, v32);
                    }
                }, 3000L);
            } else {
                Random random = new Random(982654989);
                int count = 10000;
                int threadCount = 10;
                List<Thread> threads = new ArrayList<>();
                ThrowingConsumer<Map<Embeddings, Embeddings>, Exception> commit = map -> {
                    connectionManager.execute(connection -> {
                        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO embeddings (v768,v32) VALUES " + ("(?,?),".repeat(map.size() - 1) + "(?,?)"))) {
                            int i1 = 0;
                            for (Map.Entry<Embeddings, Embeddings> embed : map.entrySet()) {
                                int index = (i1++) * 2 + 1;
                                connectionManager.setBlob(connection, preparedStatement, index, embed.getKey().getBytes());
                                connectionManager.setBlob(connection, preparedStatement, index + 1, embed.getValue().getBytes());
                            }
                            preparedStatement.execute();
                        }
                    }, 3000L);

                    map.clear();
                };
                for (int threadN_ = 0; threadN_ < threadCount; threadN_++) {
                    final int threadN = threadN_;
                    Thread thread = new Thread(() -> {
                        Map<Embeddings, Embeddings> embeddingsMap = new HashMap<>();
                        for (int i = 0; i < count / threadCount; i++) {
                            Embeddings embedding768 = newRandomEmbeddings(768, random);
                            Embeddings embedding32 = embedding768.applyPCA(32);

                            embeddingsMap.put(embedding768, embedding32);

                            if (embeddingsMap.size() >= 100) {
                                try {
                                    commit.accept(embeddingsMap);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (threadN == 0 && i % 100 == 0) {
                                System.out.println("Generating test embeddings: " + Formatter.toString(i * 100 * threadCount / (double) count, 2, true, true) + "%");
                            }
                        }
                        try {
                            commit.accept(embeddingsMap);
                        } catch (Exception e) {
                            e.printStackTrace();
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
            }

            return out;
        }
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

    @RepeatedTest(value=100)
    public void testProject() {
        int toSize = 30;
        Embeddings reducedEmbeddings1 = embeddingsRandom100.projectTo(toSize);
        Embeddings reducedEmbeddings2 = modifySlightly(embeddingsRandom100.applyPCA(toSize), new Random(89725783228892L)).projectTo(toSize);

        // Check the size of the reduced embeddings
        assertEquals(toSize, reducedEmbeddings1.getEmbeddings().size());
        assertEquals(toSize, reducedEmbeddings2.getEmbeddings().size());

        // Compute reconstruction error
        double similarity = reducedEmbeddings1.compareTo(reducedEmbeddings2);
        assertTrue(similarity > 0.8, "Similarity should be high, is " + similarity);
    }


    @Test
    public void testInvalidPCA() {
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.applyPCA(0));
        assertThrows(IllegalArgumentException.class, () -> embeddings123456.applyPCA(embeddings123456.getEmbeddings().size()));
    }
}
