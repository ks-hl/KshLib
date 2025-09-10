package dev.kshl.kshlib.function;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetConsumer {
    void consume(ResultSet connection) throws SQLException, BusyException;
}
