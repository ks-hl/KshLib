package dev.kshl.kshlib.function;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetConnectionConsumer {
    void consume(Connection connection, ResultSet resultSet) throws SQLException;
}
