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

    public LLMEmbeddingsManager(ConnectionManager connectionManager, String table) {
        if (!connectionManager.isMySQL()) {
            throw new IllegalStateException("Embeddings are only implemented on databases with VECTOR datatypes");
        }
        this.connectionManager = connectionManager;
        this.table = table;
    }

    public void init(Connection connection) throws SQLException {
        connectionManager.execute(connection,
                String.format("""
                        CREATE TABLE IF NOT EXISTS %s (
                            filename varchar(255),
                            start_index INT,
                            end_index INT,
                            content varchar(32768),
                            embedding VECTOR(768) NOT NULL
                        )""", table));
    }
}
