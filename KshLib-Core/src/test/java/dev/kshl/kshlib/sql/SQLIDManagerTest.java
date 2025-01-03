package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLIDManagerTest {
    @DatabaseTest
    public void testSQLIDManagerStr(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS id_str", 100);
        SQLIDManager.Str idManagerStr = new SQLIDManager.Str(connectionManager, "id_str");
        connectionManager.execute(idManagerStr::init, 100);

        assert idManagerStr.getValueOpt(1).isEmpty();
        assert idManagerStr.getIDOpt("test", false).isEmpty();

        int uid = idManagerStr.getIDOrInsert("test");
        assert uid > 0;
        assertEquals("test", idManagerStr.getValueOpt(uid).orElse(null));
        assertEquals(uid, idManagerStr.getIDOpt("test", false).orElse(null));
    }

    @DatabaseTest
    public void testSQLIDManagerUUID(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS id_uuid", 100);
        SQLIDManager.UUIDText idManagerUID = new SQLIDManager.UUIDText(connectionManager, "id_uuid");
        connectionManager.execute(idManagerUID::init, 100);

        UUID val1 = UUID.randomUUID();
        UUID val2 = UUID.randomUUID();

        assert idManagerUID.getIDOpt(val1, false).isEmpty();
        assert idManagerUID.getIDOpt(val2, false).isEmpty();

        int uid1 = idManagerUID.getIDOrInsert(val1);
        assert uid1 > 0;

        int uid2 = idManagerUID.getIDOrInsert(val2);
        assert uid2 > 0;

        assert uid1 != uid2;

        assertEquals(val1, idManagerUID.getValueOpt(uid1).orElse(null));
        assertEquals(val2, idManagerUID.getValueOpt(uid2).orElse(null));

        assertEquals(uid1, idManagerUID.getIDOpt(val1, false).orElse(null));
        assertEquals(uid2, idManagerUID.getIDOpt(val2, false).orElse(null));
    }
}
