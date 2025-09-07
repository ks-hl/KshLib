package dev.kshl.kshlib.sql;

import java.sql.Connection;

@FunctionalInterface
public interface ConnectionFunctionWithException<T> {
    T apply(Connection connection) throws Exception;
}
