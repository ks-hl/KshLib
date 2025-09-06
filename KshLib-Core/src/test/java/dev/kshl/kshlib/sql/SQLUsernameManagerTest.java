package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.Pair;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLUsernameManagerTest {

    @DatabaseTest
    public void testSQLUsernameManager(ConnectionManager connectionManager) throws SQLException, BusyException {
        connectionManager.execute("DROP TABLE IF EXISTS usernames", 3000L);
        SQLUsernameManager usernameManager = new SQLUsernameManager(connectionManager, "usernames");

        // Initialize the table
        connectionManager.execute(usernameManager::init, 3000L);

        // Test updating username for the first time
        usernameManager.updateUsername(1, "testuser");
        Optional<String> retrievedUsername = usernameManager.getUsername(1);
        assertTrue(retrievedUsername.isPresent());
        assertEquals("testuser", retrievedUsername.get());

        // Test retrieving UID by username (case insensitive check)
        Optional<Pair<Integer, String>> retrievedUIDAndUsername = usernameManager.getUIDAndUsername("TESTUSER");
        assertTrue(retrievedUIDAndUsername.isPresent());
        assertEquals(1, retrievedUIDAndUsername.get().getLeft().intValue());
        assertEquals("testuser", retrievedUIDAndUsername.get().getRight());

        // Test updating the username with a different UID (more recent entry)
        usernameManager.updateUsername(2, "testuser");
        var retrievedUID = usernameManager.getUID("testuser");
        assertTrue(retrievedUID.isPresent());
        assertEquals(2, retrievedUID.get().intValue()); // Should return the most recent UID

        // Test for non-existent username
        retrievedUID = usernameManager.getUID("nonexistentuser");
        assertFalse(retrievedUID.isPresent());

        // Test inserting another user with a different case
        usernameManager.updateUsername(3, "AnotherUser");
        retrievedUID = usernameManager.getUID("anotheruser");
        assertTrue(retrievedUID.isPresent());
        assertEquals(3, retrievedUID.get().intValue());

        // Verify most recent entry is returned when multiple entries exist for the same username
        usernameManager.updateUsername(4, "testuser");
        retrievedUID = usernameManager.getUID("testuser");
        assertTrue(retrievedUID.isPresent());
        assertEquals(4, retrievedUID.get().intValue()); // Should return the most recent UID (4)

        // Test for handling null in getUsername
        Optional<String> nullUsername = usernameManager.getUsername(-1);
        assertFalse(nullUsername.isPresent());

        // Test for handling null in getUID
        Optional<Integer> nullUID = usernameManager.getUID(null);
        assertFalse(nullUID.isPresent());

        // Test if a user changes their username
        usernameManager.updateUsername(4, "newuser");
        retrievedUID = usernameManager.getUID("newuser");
        assertTrue(retrievedUID.isPresent());
        assertEquals(4, retrievedUID.get().intValue()); // Should return the updated UID (4)

        // Ensure old username still returns the UID
        retrievedUID = usernameManager.getUID("testuser");
        assertTrue(retrievedUID.isPresent());

        // Ensure the username associated with UID 4 is updated
        retrievedUsername = usernameManager.getUsername(4);
        assertTrue(retrievedUsername.isPresent());
        assertEquals("newuser", retrievedUsername.get());
    }
}
