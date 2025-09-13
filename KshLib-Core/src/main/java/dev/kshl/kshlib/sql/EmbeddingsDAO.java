package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.Bits;
import dev.kshl.kshlib.misc.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmbeddingsDAO {
    private final ConnectionManager sql;
    private final SQLIDManager.Str pathIDManager;
    private final String table;

    public EmbeddingsDAO(ConnectionManager connectionManager, String table) {
        if (!connectionManager.isMySQL()) {
            throw new IllegalStateException("Embeddings are only implemented on databases with VECTOR datatypes");
        }
        this.sql = connectionManager;
        this.table = table;
        this.pathIDManager = new SQLIDManager.Str(connectionManager, table + "_paths", 4096) {
            @Override
            protected String getTableMetaDataColumns() {
                return "file_hash BINARY(32)";
            }
        };
    }

    public void init(Connection connection) throws SQLException {
        sql.execute(connection,
                String.format("""
                        CREATE TABLE IF NOT EXISTS %s (
                            path INT,
                            start_index INT,
                            end_index INT,
                            content TEXT,
                            embedding VECTOR(768) NOT NULL
                        )""", table));
        sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_vector ON %s (embedding)", table, table));
        sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_path ON %s (path)", table, table));
        pathIDManager.init(connection);
    }

    public byte[] getFileHash(String path) throws SQLException, BusyException {
        int pathID = pathIDManager.getIDOpt(path, false).orElse(-1);
        if (pathID <= 0) return null;
        return sql.query("SELECT file_hash FROM " + pathIDManager.getTableName() + " WHERE id=?", rs -> {
            if (!rs.next()) return null;
            return rs.getBytes("file_hash");
        }, 3000L, pathID);
    }

    public void dropFile(String path) throws SQLException, BusyException {
        int pathID = pathIDManager.getIDOrInsert(path);
        sql.execute("DELETE FROM " + table + " WHERE path=?", 3000L, pathID);
        sql.execute("UPDATE " + pathIDManager.getTableName() + " SET file_hash=null WHERE id=?", 3000L, pathID);
    }

    public boolean upsertFileHash(String path, byte[] hash) throws SQLException, BusyException {
        int pathID = pathIDManager.getIDOrInsert(path);
        return sql.executeReturnRows("UPDATE " + pathIDManager.getTableName() + " SET file_hash=? WHERE id=?", 3000L, hash, pathID) > 0;
    }

    public void upsertFileBlock(String path, int startIndex, int endIndex, String content, float[] embedding) throws SQLException, BusyException {
        int pathID = pathIDManager.getIDOrInsert(path);
        sql.execute("INSERT INTO " + table + " (path,start_index,end_index,content,embedding) VALUES (?,?,?,?,VEC_FromText(?))", 3000L,
                pathID, startIndex, endIndex, content, Arrays.toString(embedding));
    }

    public List<String> getFiles() throws SQLException, BusyException {
        return sql.query("SELECT value FROM " + pathIDManager.getTableName(), rs -> {
            List<String> out = new ArrayList<>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            return out;
        }, 3000L);
    }

    public List<Pair<String, Float>> getNeighbors(float[] embedding, int limit) throws SQLException, BusyException {
        return sql.query(String.format("""
                SELECT content, VEC_DISTANCE_EUCLIDEAN(embedding, VEC_FromText(?)) AS distance
                FROM %s
                ORDER BY distance
                LIMIT ?""", table), rs -> {
            List<Pair<String, Float>> out = new ArrayList<>();

            while (rs.next()) {
                out.add(new Pair<>(rs.getString("content"), rs.getFloat("distance")));
            }

            return out;
        }, 3000L, Arrays.toString(embedding), limit);
    }
}
