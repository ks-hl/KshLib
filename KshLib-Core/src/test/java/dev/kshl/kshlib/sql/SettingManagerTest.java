
package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SettingManagerTest {
    @DatabaseTest
    public void testSQLSettingsInteger(ConnectionManager connectionManager) throws SQLException, BusyException {
        SettingManager.Int settingManager = new SettingManager.Int(connectionManager, "setting_manager_int", -1);
        connectionManager.execute(settingManager::init, 3000L);

        assertEquals(-1, settingManager.get(1));

        settingManager.set(2, 3);
        assertEquals(-1, settingManager.get(1));
        assertEquals(3, settingManager.get(2));

        settingManager.increment(2);
        assertEquals(4, settingManager.get(2));

        settingManager.decrement(2);
        assertEquals(3, settingManager.get(2));

        settingManager.increment(2, -2);
        assertEquals(1, settingManager.get(2));
    }
}
