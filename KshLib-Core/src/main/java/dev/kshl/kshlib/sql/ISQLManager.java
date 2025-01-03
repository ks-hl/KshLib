package dev.kshl.kshlib.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ISQLManager {
    default void validateTableName(String table) {
        if (!table.matches("[\\w_]+")) throw new IllegalArgumentException("Invalid table name " + table);
    }

    void init(Connection connection) throws SQLException;
}
