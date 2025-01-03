package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementConsumer {
    void consume(PreparedStatement preparedStatement) throws SQLException, BusyException;
}
