package dev.kshl.kshlib.sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

class TestConnectionManager extends ConnectionManager {

    public TestConnectionManager(File sqliteFile) throws IOException, SQLException, ClassNotFoundException {
        super(sqliteFile);

        init();
    }

    public TestConnectionManager(String uri, String database, String user, String pwd, int poolSize) throws SQLException, ClassNotFoundException {
        super(uri, database, user, pwd, poolSize);

        init();
    }

    @Override
    protected boolean isDebug() {
        return false;
    }

    @Override
    protected void init(Connection connection) throws SQLException {
        execute(connection, "DROP TABLE IF EXISTS test_table");
        execute(connection, "CREATE TABLE IF NOT EXISTS test_table (uid INT, time BIGINT)");
    }

    @Override
    protected void debug(String line) {
        System.out.println(line);
    }

    @Override
    protected boolean checkAsync() {
        return true;
    }
}
