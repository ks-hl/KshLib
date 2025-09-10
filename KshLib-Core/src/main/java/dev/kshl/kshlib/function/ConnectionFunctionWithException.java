package dev.kshl.kshlib.function;

import java.sql.Connection;

@FunctionalInterface
public interface ConnectionFunctionWithException<T> {
    T apply(Connection connection) throws Exception;
}
