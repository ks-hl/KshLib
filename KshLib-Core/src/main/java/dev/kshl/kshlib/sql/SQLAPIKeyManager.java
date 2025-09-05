package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.crypto.CodeGenerator;
import dev.kshl.kshlib.exceptions.BusyException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SQLAPIKeyManager {
    private final ConnectionManager connectionManager;
    private final int length;
    private final String table;
    private final SQLPasswordManager passwordManager;

    public SQLAPIKeyManager(ConnectionManager connectionManager, String table, int length) throws NoSuchAlgorithmException {
        if (length < 32) throw new IllegalArgumentException("Can't have token length < 32");
        this.length = length;
        this.connectionManager = connectionManager;
        this.table = table;
        this.passwordManager = new SQLPasswordManager(connectionManager, table, SQLPasswordManager.Type.PASSWORD) {
            @Override
            protected String getCreateTableStatement() {
                return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (created BIGINT, uid INT PRIMARY KEY, last_changed BIGINT, hash BLOB, description TEXT)";
            }
        };
    }

    public void init(Connection connection) throws SQLException {
        passwordManager.init(connection);
    }

    public APIKeyPair createNew(String description) throws SQLException, BusyException {
        String secret = CodeGenerator.generateSecret(length, true, false, false);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10000; i++) {
            try {
                int id = random.nextInt(1000000, Integer.MAX_VALUE);
                passwordManager.setPassword(id, secret, true, 0);
                updateDescription(id, description);
                return new APIKeyPair(id, secret);
            } catch (SQLException e) {
                if (!ConnectionManager.isConstraintViolation(e)) throw e;
            }
        }

        throw new IllegalStateException("Failed to generate key after 10000 attempts");
    }

    public boolean test(int id, String secret) throws SQLException, BusyException {
        return passwordManager.testPassword(id, secret);
    }

    public boolean test(APIKeyPair apiKeyPair) throws SQLException, BusyException {
        return test(apiKeyPair.id(), apiKeyPair.secret());
    }

    public boolean contains(int id) throws SQLException, BusyException {
        return passwordManager.contains(id);
    }

    public Map<Integer, String> list() throws SQLException, BusyException {
        return connectionManager.query("SELECT uid,description FROM " + table, rs -> {
            Map<Integer, String> out = new HashMap<>();
            while (rs.next()) out.put(rs.getInt(1), rs.getString(2));
            return out;
        }, 3000);
    }

    public void updateDescription(int id, String description) throws SQLException, BusyException {
        connectionManager.execute("UPDATE " + table + " SET description=? WHERE uid=?", 3000, description, id);
    }

    public void revoke(int id) throws SQLException, BusyException {
        passwordManager.remove(id);
    }

    public record APIKeyPair(int id, String secret) {
        public String combined() {
            return id() + ":" + secret();
        }

        public String base64() {
            return Base64.getEncoder().encodeToString(combined().getBytes());
        }
    }

    public String getTableName() {
        return passwordManager.getTableName();
    }
}
