package dev.kshl.kshlib.function;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionConsumer {
    void accept(Connection connection) throws SQLException, BusyException;
}
