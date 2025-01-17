package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import dev.kshl.kshlib.llm.embed.ByteEmbeddings;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;
import dev.kshl.kshlib.llm.embed.ShortEmbeddings;
import dev.kshl.kshlib.misc.Pair;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLMEmbeddingsManager {
    private final ConnectionManager connectionManager;
    private final String table;
    private final Quant quant;
    private Map<Integer, AbstractEmbeddings> embeddings;

    public LLMEmbeddingsManager(ConnectionManager connectionManager, String table, Quant quant) {
        this.connectionManager = connectionManager;
        this.table = table;
        this.quant = quant;
    }

    public void init(Connection connection) throws SQLException {
        connectionManager.execute(connection,
                "CREATE TABLE IF NOT EXISTS " + table + " (" +
                        "id INTEGER PRIMARY KEY " + connectionManager.autoincrement() + ", " +
                        "vector BLOB NOT NULL," +
                        "hash INTEGER" +
                        ")");

        this.embeddings = getAll(connection);
    }

    public int put(AbstractEmbeddings embeddings) throws SQLException, BusyException {
        embeddings = quant(embeddings);
        Integer id = getID(embeddings);
        if (id != null) return id;

        id = connectionManager.executeReturnGenerated("INSERT INTO " + table + " (vector, hash) VALUES (?,?)", 3000L, embeddings.getBytes(), embeddings.hashCode());

        this.embeddings.put(id, embeddings);
        return id;
    }

    private Integer getID(AbstractEmbeddings embeddings) throws SQLException, BusyException {
        return connectionManager.query("SELECT * FROM " + table + " WHERE hash=?", rs -> {
            AbstractEmbeddings quantEmbeddings = quant(embeddings);
            while (rs.next()) {
                Pair<Integer, AbstractEmbeddings> other = fromResultSet(rs);
                if (other.getValue().equals(quantEmbeddings)) return other.getKey();
            }
            return null;
        }, 3000L, embeddings.hashCode());
    }

    private Pair<Integer, AbstractEmbeddings> fromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        byte[] blob = connectionManager.getBlob(rs, "vector");

        return new Pair<>(id, switch (quant) {
            case Q8 -> ByteEmbeddings.fromBytes(blob);
            case Q16 -> ShortEmbeddings.fromBytes(blob);
            case FP32 -> FloatEmbeddings.fromBytes(blob);
        });
    }

    private AbstractEmbeddings quant(AbstractEmbeddings abstractEmbeddings) {
        return switch (quant) {
            case Q8 -> abstractEmbeddings.toByteEmbeddings();
            case Q16 -> abstractEmbeddings.toShortEmbeddings();
            case FP32 -> abstractEmbeddings;
        };
    }

    private Map<Integer, AbstractEmbeddings> getAll(Connection connection) throws SQLException {
        Map<Integer, AbstractEmbeddings> out = new HashMap<>();
        connectionManager.query(connection, "SELECT * FROM " + table, rs -> {
            while (rs.next()) {
                Pair<Integer, AbstractEmbeddings> embed = fromResultSet(rs);
                out.put(embed.getKey(), embed.getValue());
            }
        });
        return out;
    }

    public List<Map.Entry<Integer, Double>> query(AbstractEmbeddings embeddings, float minimumSimilarity) {
        Map<Integer, Double> comparisons = new HashMap<>();
        for (Map.Entry<Integer, AbstractEmbeddings> entry : this.embeddings.entrySet()) {
            double comparison = entry.getValue().compareCosine(embeddings);
            if (comparison < minimumSimilarity) continue;

            comparisons.put(entry.getKey(), comparison);
        }
        List<Map.Entry<Integer, Double>> out = new ArrayList<>(comparisons.entrySet());
        out.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return out;
    }

    public enum Quant {
        Q8, Q16, FP32
    }
}
