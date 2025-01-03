package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFunctionWithException<T> {
    T apply(Connection connection) throws SQLException, BusyException, InterruptedException;
}
