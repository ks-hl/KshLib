package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionConsumer;
import dev.kshl.kshlib.misc.Pair;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLUsernameManagerTest {

    /**
     * Helper to build a clean manager and init the table for each test.
     */
    private SQLUsernameManager newManager(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS usernames", 3000L);
        SQLUsernameManager usernameManager = new SQLUsernameManager(connectionManager, "usernames") {
            @Override
            void cache(Integer uid, String username) {
                // disable external caching side-effects for tests
                super.cache(uid, username);
            }
        };
        connectionManager.execute((ConnectionConsumer) connection -> usernameManager.init(connection, false), 3000L);
        return usernameManager;
    }

    // --- Basic initialization / empty state -------------------------------------------------------
    @DatabaseTest
    public void init_createsEmptyState(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        assertTrue(m.getUsername(1).isEmpty());
        assertTrue(m.getUID("nobody").isEmpty());
    }

    // --- Insert and retrieve (UID -> username) ---------------------------------------------------
    @DatabaseTest
    public void updateAndGetUsername(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(1, "testuser");
        Optional<String> u = m.getUsername(1);
        assertTrue(u.isPresent());
        assertEquals("testuser", u.get());
    }

    // --- Case-insensitive username lookup and (UID, username) pair -------------------------------
    @DatabaseTest
    public void getUIDAndUsername_caseInsensitive(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(1, "testuser");
        Optional<Pair<Integer, String>> p = m.getUIDAndUsername("TESTUSER");
        assertTrue(p.isPresent());
        assertEquals(1, p.get().getLeft().intValue());
        assertEquals("testuser", p.get().getRight());
    }

    // --- “Most recent UID wins” for the same username --------------------------------------------
    @DatabaseTest
    public void mostRecentUIDWinsForSameUsername(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(1, "testuser");
        m.updateUsername(2, "testuser");
        var uid = m.getUID("testuser");
        assertTrue(uid.isPresent());
        assertEquals(2, uid.get().intValue());

        // Insert another, ensure it becomes most recent
        m.updateUsername(4, "testuser");
        uid = m.getUID("testuser");
        assertTrue(uid.isPresent());
        assertEquals(4, uid.get().intValue());
    }

    // --- Non-existent username returns empty ------------------------------------------------------
    @DatabaseTest
    public void getUID_nonExistent_returnsEmpty(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        assertTrue(m.getUID("nonexistentuser").isEmpty());
    }

    // --- Case-insensitive insert & lookup for different users ------------------------------------
    @DatabaseTest
    public void caseInsensitiveLookup_differentUser(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(3, "AnotherUser");
        var uid = m.getUID("anotheruser");
        assertTrue(uid.isPresent());
        assertEquals(3, uid.get().intValue());
    }

    // --- Null / invalid argument handling ---------------------------------------------------------
    @DatabaseTest
    public void nullAndInvalidHandling(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        assertTrue(m.getUsername(-1).isEmpty());
        assertTrue(m.getUID(null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> m.updateUsername(0, "name"));
        assertThrows(NullPointerException.class, () -> m.updateUsername(1, null));
    }

    // --- Username change: old username maps to latest UID & new username is reflected ------------
    @DatabaseTest
    public void usernameChangeOldAliasResolvesToLatestUID(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(4, "testuser");
        m.updateUsername(4, "newuser");

        // New name resolves to UID 4
        var uid = m.getUID("newuser");
        assertTrue(uid.isPresent());
        assertEquals(4, uid.get().intValue());

        // Old alias should still resolve to the latest (UID 4) and latest username (“newuser”)
        var pair = m.getUIDAndUsername("testuser").orElseThrow();
        assertEquals(4, pair.getLeft().intValue());
        assertEquals("newuser", pair.getRight());

        // UID -> username path reflects the latest username
        var name = m.getUsername(4).orElseThrow();
        assertEquals("newuser", name);
    }

    // --- Repeat history for a single UID ----------------------------------------------------------
    @DatabaseTest
    public void repeatHistoryForSingleUID(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        m.updateUsername(1, "1");
        assertEquals("1", m.getUsername(1).orElseThrow());
        m.updateUsername(1, "2");
        assertEquals("2", m.getUsername(1).orElseThrow());
        m.updateUsername(1, "1");
        assertEquals("1", m.getUsername(1).orElseThrow());
    }

    // --- Recent usernames lifecycle (extra coverage) ----------------------------------------------
    @DatabaseTest
    public void recentUsernames_mustBeInitialized_thenQueryable(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        // Not initialized -> methods should throw
        assertThrows(IllegalStateException.class, () -> m.getRecentUsernamesStartingWith("t"));
        assertThrows(IllegalStateException.class, m::getRecentUsernamesSize);

        // Seed some data
        m.updateUsername(10, "Tom");
        m.updateUsername(11, "tony");
        m.updateUsername(12, "Alice");
        m.updateUsername(13, "ALISON");
        m.updateUsername(13, "ALISON2");

        // Populate and query
        cm.execute((ConnectionConsumer) connection -> m.populateRecentUsernames(connection, 5000), 3000L);
        cm.query("SELECT * FROM usernames", rs -> {
            while (rs.next()) {
                System.out.println("Snowflake: " + rs.getLong("time") + ", Username: " + rs.getString("username"));
            }
        }, 3000L);
        assertEquals(4, m.getRecentUsernamesSize());

        Set<String> tNames = m.getRecentUsernamesStartingWith("to");
        assertTrue(tNames.contains("Tom"));
        assertTrue(tNames.contains("tony"));

        // Cannot populate twice
        assertThrows(IllegalStateException.class, () ->
                cm.execute((ConnectionConsumer) connection -> m.populateRecentUsernames(connection, 5000), 3000L)
        );

        // Ensure replaced usernames are not in recent
        m.updateUsername(10, "Tom2");
        assertFalse(m.getRecentUsernamesStartingWith("to").contains("Tom"));
        assertTrue(m.getRecentUsernamesStartingWith("to").contains("Tom2"));
        assertFalse(m.getRecentUsernamesStartingWith("al").contains("ALISON"));
        assertTrue(m.getRecentUsernamesStartingWith("al").contains("ALISON2"));
    }

    // --- UUID bridging helpers don’t crash for unknowns (extra coverage) -------------------------
    @DatabaseTest
    public void uuidBridging_unknownsReturnEmpty(ConnectionManager cm) throws Exception {
        SQLUsernameManager m = newManager(cm);

        // Minimal fake for SQLIDManager.UUIDText that returns empty for any id/uuid
        SQLIDManager.UUIDText fake = new SQLIDManager.UUIDText(cm, "ids") {
            @Override
            public Optional<UUID> getValueOpt(int id) {
                return Optional.empty();
            }

            @Override
            public Optional<Integer> getIDOpt(UUID uuid, boolean createIfMissing) {
                return Optional.empty();
            }
        };

        assertTrue(m.getUUID("ghost", fake).isEmpty());
        assertTrue(m.getUUIDAndUsername("ghost", fake).isEmpty());
        assertTrue(m.getUsername(UUID.randomUUID(), fake).isEmpty());
    }
}
