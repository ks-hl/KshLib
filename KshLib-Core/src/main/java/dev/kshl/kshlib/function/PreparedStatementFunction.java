package dev.kshl.kshlib.function;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementFunction<T> {
    T apply(PreparedStatement preparedStatement) throws SQLException;
}
