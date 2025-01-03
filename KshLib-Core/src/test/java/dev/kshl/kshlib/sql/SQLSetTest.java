
package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.SQLException;

public class SQLSetTest {
    @DatabaseTest
    public void testSQLSet(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS sql_set", 100);
        SQLSet.Int sqlSet = new SQLSet.Int(connectionManager, "sql_set", false);
        connectionManager.execute(sqlSet::init, 100);

        assert !sqlSet.contains(1);
        assert sqlSet.add(1);
        assert sqlSet.contains(1);
        assert sqlSet.remove(1);
    }
}
