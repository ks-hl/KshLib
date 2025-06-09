
package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingManagerTest {
    @DatabaseTest
    public void testSQLSettingsInteger(ConnectionManager connectionManager) throws SQLException, BusyException {
        SettingManager.Int settingManager = new SettingManager.Int(connectionManager, "setting_manager_int", false, -1);
        connectionManager.execute(settingManager::init, 3000L);

        assertEquals(-1, settingManager.get(1));

        settingManager.set(2, 3);
        assertEquals(-1, settingManager.get(1));
        assertEquals(3, settingManager.get(2));

        settingManager.inc(2);
        assertEquals(4, settingManager.get(2));

        settingManager.dec(2);
        assertEquals(3, settingManager.get(2));

        settingManager.add(2, -2);
        assertEquals(1, settingManager.get(2));

        assertThrows(IllegalArgumentException.class, () -> settingManager.set(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> settingManager.set(1, -1, 0));
    }

    @DatabaseTest
    public void testSQLSettingsIntegerMultiple(ConnectionManager connectionManager) throws SQLException, BusyException {
        SettingManager.Int settingManager = new SettingManager.Int(connectionManager, "setting_manager_int_multi", true, null);
        connectionManager.execute(settingManager::init, 3000L);

        assertNull(settingManager.get(1, 1));

        settingManager.set(2, 1, 3);
        settingManager.set(2, 2, 4);
        settingManager.set(3, 2, 5);

        assertNull(settingManager.get(1, 1));
        assertNull(settingManager.get(3, 1));
        assertEquals(3, settingManager.get(2, 1));
        assertEquals(4, settingManager.get(2, 2));
        assertEquals(5, settingManager.get(3, 2));

        settingManager.inc(2, 1);
        assertEquals(4, settingManager.get(2, 1));

        settingManager.dec(2, 1);
        assertEquals(3, settingManager.get(2, 1));

        settingManager.add(2, 1, -2);
        assertEquals(1, settingManager.get(2, 1));

        assertThrows(IllegalArgumentException.class, () -> settingManager.set(1, 0, 0));
    }

    @DatabaseTest
    public void testSQLSettingsBoolean(ConnectionManager connectionManager) throws SQLException, BusyException {
        SettingManager.Bool settingManager = new SettingManager.Bool(connectionManager, "setting_manager_int", false, false);
        connectionManager.execute(settingManager::init, 3000L);

        assertFalse(settingManager.get(1));
        assertTrue(settingManager.toggle(1));
        assertFalse(settingManager.toggle(1));

        settingManager.set(1, true);
        settingManager.set(1, true);
        assertTrue(settingManager.toggle(1));
    }
}
