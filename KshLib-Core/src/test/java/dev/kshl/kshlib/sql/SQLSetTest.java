
package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLSetTest {
    @DatabaseTest
    public void testSQLSet(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS sql_set", 100);
        SQLSet.Int sqlSet = new SQLSet.Int(connectionManager, "sql_set", false);
        connectionManager.execute(sqlSet::init, 100);

        assertTrue(!sqlSet.contains(1));
        assertTrue(sqlSet.add(1));
        assertFalse(sqlSet.add(1));
        assertTrue(sqlSet.contains(1));
        assertTrue(sqlSet.remove(1));
        assertFalse(sqlSet.remove(1));
    }
}
