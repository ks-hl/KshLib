package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunction;
import dev.kshl.kshlib.function.PreparedStatementFunction;
import dev.kshl.kshlib.function.ResultSetFunction;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A utility/builder class that provides different options for performing database lookups.
 * <br>
 * NOTE: The parameter count checker is only enabled in RESULT_SET and VOID modes, although args can be provided in PREPARED_STATEMENT mode.
 *
 * @param <T>
 */
public class StatementBuilder<T> {
    private final ConnectionManager connectionManager;
    private final ConnectionFunction<T> connectionFunction;
    @Getter
    private final FunctionType functionType;
    @Getter
    private Action action = Action.FUNCTION;
    private int result;
    private Object[] args;
    @Getter
    private boolean used;
    private final boolean readOnly;

    StatementBuilder(ConnectionManager connectionManager, ConnectionFunction<T> connectionFunction) {
        this(FunctionType.CONNECTION, connectionManager, null, connectionFunction, null, null);
    }

    StatementBuilder(ConnectionManager connectionManager, String statement, PreparedStatementFunction<T> preparedStatementFunction) {
        this(FunctionType.PREPARED_STATEMENT, connectionManager, Objects.requireNonNull(statement, "Statement must not be null"), null, preparedStatementFunction, null);
    }

    StatementBuilder(ConnectionManager connectionManager, String statement, ResultSetFunction<T> resultSetFunction) {
        this(FunctionType.RESULT_SET, connectionManager, Objects.requireNonNull(statement, "Statement must not be null"), null, null, resultSetFunction);
    }

    StatementBuilder(ConnectionManager connectionManager, String statement) {
        this(FunctionType.VOID, connectionManager, statement, null, null, rs -> null);
    }

    private StatementBuilder(FunctionType functionType,
                             ConnectionManager connectionManager,
                             String statement,
                             ConnectionFunction<T> connectionFunction,
                             PreparedStatementFunction<T> preparedStatementFunction,
                             ResultSetFunction<T> resultSetFunction) {
        this.functionType = functionType;
        this.connectionManager = connectionManager;
        if (resultSetFunction != null) {
            preparedStatementFunction = adaptToPreparedStatement(resultSetFunction);
        }
        if (preparedStatementFunction != null) {
            connectionFunction = adaptToConnection(statement, preparedStatementFunction);
        }
        this.connectionFunction = connectionFunction;
        this.readOnly = statement != null && statement.trim().toLowerCase().startsWith("select ");
    }

    public StatementBuilder<T> args(Object... args) {
        if (this.functionType == FunctionType.CONNECTION || this.functionType == FunctionType.PREPARED_STATEMENT) {
            throw new UnsupportedOperationException("args are not applicable to ConnectionConsumer/Functions or PreparedStatementConsumer/Functions");
        }
        this.args = args;
        return this;
    }

    public T executeQuery(Connection connection) throws SQLException {
        checkUsed();
        return connectionFunction.apply(connection);
    }

    public T executeQuery(long waitMillis) throws SQLException, BusyException {
        checkUsed();
        return connectionManager.execute(connectionFunction, waitMillis, readOnly);
    }

    public int executeReturnGenerated(Connection connection) throws SQLException {
        try {
            return executeReturnGenerated(connection, 0L);
        } catch (BusyException e) {
            // Not thrown
            throw new RuntimeException(e);
        }
    }

    public int executeReturnGenerated(long waitMillis) throws SQLException, BusyException {
        return executeReturnGenerated(null, waitMillis);
    }

    private int executeReturnGenerated(Connection connection, Long waitMillis) throws SQLException, BusyException {
        if (this.functionType != FunctionType.VOID) {
            throw new UnsupportedOperationException("executeReturnGenerated is only applicable to Void");
        }
        this.action = Action.GENERATED;
        if (connection != null) executeQuery(connection);
        else executeQuery(waitMillis);
        return result;
    }

    public int executeReturnRows(Connection connection) throws SQLException {
        try {
            return executeReturnRows(connection, 0L);
        } catch (BusyException e) {
            // Not thrown
            throw new RuntimeException(e);
        }
    }

    public int executeReturnRows(long waitMillis) throws SQLException, BusyException {
        return executeReturnRows(null, waitMillis);
    }

    private int executeReturnRows(Connection connection, Long waitMillis) throws SQLException, BusyException {
        if (this.functionType != FunctionType.VOID) {
            throw new UnsupportedOperationException("executeReturnRows is only applicable to Void");
        }
        this.action = Action.ROWS;
        if (connection != null) executeQuery(connection);
        else executeQuery(waitMillis);
        return result;
    }

    private void checkUsed() throws IllegalStateException {
        if (used) {
            throw new IllegalStateException("StatementBuilder is not reusable. This instance has already been executed.");
        }
        used = true;
    }

    private ConnectionFunction<T> adaptToConnection(String statement, PreparedStatementFunction<T> preparedStatementFunction) {
        return connection -> {
            connectionManager.debugSQLStatement((readOnly ? "[READONLY] " : "") + statement, args);
            PreparedStatement preparedStatement;
            if (action == Action.GENERATED) {
                preparedStatement = connection.prepareStatement(statement, PreparedStatement.RETURN_GENERATED_KEYS);
            } else {
                preparedStatement = connection.prepareStatement(statement);
            }
            try {
                if (this.args != null && this.args.length > 0) {
                    connectionManager.prepare(preparedStatement, args);
                }
                return preparedStatementFunction.apply(preparedStatement);
            } finally {
                preparedStatement.close();
            }
        };
    }

    private PreparedStatementFunction<T> adaptToPreparedStatement(ResultSetFunction<T> resultSetFunction) {
        return preparedStatement -> {
            connectionManager.checkEnoughArguments(preparedStatement, args);
            if (action == Action.GENERATED) {
                preparedStatement.execute();
                try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    if (rs.next()) result = rs.getInt(1);
                }
                return null;
            } else if (action == Action.ROWS) {
                this.result = preparedStatement.executeUpdate();
                return null;
            } else {
                if (functionType == FunctionType.VOID) {
                    preparedStatement.execute();
                    return resultSetFunction.apply(null);
                } else {
                    try (ResultSet rs = preparedStatement.executeQuery()) {
                        return resultSetFunction.apply(rs);
                    }
                }
            }
        };
    }

    private enum Action {
        FUNCTION, GENERATED, ROWS
    }

    private enum FunctionType {
        CONNECTION, PREPARED_STATEMENT, VOID, RESULT_SET
    }
}