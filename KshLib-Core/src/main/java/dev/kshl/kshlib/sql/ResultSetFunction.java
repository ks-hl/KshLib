package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetFunction<T> {
    T apply(ResultSet connection) throws SQLException, BusyException;
}
