package dev.kshl.kshlib.function;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetFunction<T> {
    T apply(ResultSet connection) throws SQLException, BusyException;
}
