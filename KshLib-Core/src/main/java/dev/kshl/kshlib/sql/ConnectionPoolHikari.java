package dev.kshl.kshlib.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunctionWithException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConnectionPoolHikari extends ConnectionPool {
    private final ThreadLocal<Connection> issuedConnections = new ThreadLocal<>();
    private final HikariDataSource hikari;

    public ConnectionPoolHikari(String host, String database, String user, String pwd, int poolSize) throws ClassNotFoundException {
        super();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + "/" + database);
        config.setUsername(user);
        config.setPassword(pwd);

        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle((int) Math.max(1, Math.round(poolSize / 4D)));
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(5));
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));

        config.setPoolName("KshLibPool-" + UUID.randomUUID().toString().replace("-", ""));

        this.hikari = new HikariDataSource(config);
    }

    @Override
    protected <T> T executeWithException_(ConnectionFunctionWithException<T> connectionFunction, long wait) throws Exception {
        if (isClosing()) {
            throw new BusyException("Database closing");
        }

        Connection conn = issuedConnections.get();
        boolean isLeader = (conn == null); // Whether this Thread will be getting a new Connection instead of using a cached one

        if (isLeader) {
            try {
                conn = hikari.getConnection();
                issuedConnections.set(conn);
            } catch (SQLException e) {
                throw new BusyException("Unable to get connection from pool: " + e.getMessage());
            }
        }

        try {
            return connectionFunction.apply(conn);
        } finally {
            if (isLeader) {
                issuedConnections.remove();
                try {
                    conn.close(); // returns it to Hikari pool
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public int getActiveConnections() {
        return hikari.getHikariPoolMXBean().getActiveConnections();
    }

    @Override
    public int getAllEstablishedConnections() {
        return hikari.getHikariPoolMXBean().getTotalConnections();
    }

    @Override
    public void closeInternal() {
        if (hikari != null) hikari.close();
    }

    @Override
    protected void checkDriver() {
    }

    @Override
    public boolean isMySQL() {
        return true;
    }
}
