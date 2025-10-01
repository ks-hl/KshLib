package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.function.ConnectionConsumer;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLSessionTokenUUIDManagerTest {

    /**
     * Unique-ish table name per test run to avoid collisions across methods/parallelism.
     */
    private static String randTable() {
        return "table_" + UUID.randomUUID().toString().replace("-", "");
    }

    @DatabaseTest
    void generateNew_and_test_validToken_roundTrip(ConnectionManager cm) throws Exception {
        String table = randTable();
        var mgr = new SQLSessionTokenUUIDManager(cm, table, Duration.ofMinutes(10).toMillis(), false);
        cm.execute((ConnectionConsumer) mgr::init, 1L);

        int uid = 42;
        var tok = mgr.generateNew(uid);
        assertEquals(uid, tok.uid());
        assertEquals(43, tok.token().length(), "Base64url length for 32 bytes should be 43 chars (no padding)");
        assertNotNull(tok.token_id());
        assertNull(tok.ip());

        // Valid
        Optional<Integer> ok = mgr.test(tok.token_id(), tok.token());
        assertTrue(ok.isPresent());
        assertEquals(uid, ok.get());

        // Wrong token
        Optional<Integer> bad = mgr.test(tok.token_id(), tok.token() + "x");
        assertTrue(bad.isEmpty());
    }

    @DatabaseTest
    void ipSticky_rules_enforced(ConnectionManager cm) throws Exception {
        String table = randTable();
        var mgrSticky = new SQLSessionTokenUUIDManager(cm, table, Duration.ofMinutes(10).toMillis(), true);
        cm.execute((ConnectionConsumer) mgrSticky::init, 1L);

        // Must provide IP when ipSticky=true
        assertThrows(IllegalArgumentException.class, () -> mgrSticky.generateNew(1, null));

        var tok = mgrSticky.generateNew(7, "1.2.3.4");
        assertEquals("1.2.3.4", tok.ip());

        // Valid with correct IP
        assertEquals(7, mgrSticky.test(tok.token_id(), tok.token(), "1.2.3.4").orElse(-1));

        // Invalid with different IP
        assertTrue(mgrSticky.test(tok.token_id(), tok.token(), "5.6.7.8").isEmpty());

        // When ipSticky=false, providing an IP should be rejected
        var mgrLoose = new SQLSessionTokenUUIDManager(cm, randTable(), Duration.ofMinutes(10).toMillis(), false);
        cm.execute((ConnectionConsumer) mgrLoose::init, 1L);
        assertThrows(IllegalArgumentException.class, () -> mgrLoose.generateNew(9, "9.9.9.9"));
        assertThrows(IllegalArgumentException.class, () -> mgrLoose.test(UUID.randomUUID(), "abc", "9.9.9.9"));
    }

    @DatabaseTest
    @Timeout(5)
    void expired_tokens_become_invalid_and_cache_prunes(ConnectionManager cm) throws Exception {
        String table = randTable();
        long shortDurationMs = 75;
        var mgr = new SQLSessionTokenUUIDManager(cm, table, shortDurationMs, false);
        cm.execute((ConnectionConsumer) mgr::init, 1L);

        var tok = mgr.generateNew(101);
        // Initially valid
        assertTrue(mgr.test(tok.token_id(), tok.token()).isPresent());

        // Wait past expiry
        Thread.sleep(shortDurationMs + 50);

        // Should now be invalid
        assertTrue(mgr.test(tok.token_id(), tok.token()).isEmpty());
        assertTrue(mgr.test(tok.token_id(), tok.token()).isEmpty());
    }

    @DatabaseTest
    void remove_by_uid_and_by_uid_tokenId(ConnectionManager cm) throws Exception {
        String table = randTable();
        var mgr = new SQLSessionTokenUUIDManager(cm, table, Duration.ofMinutes(10).toMillis(), false);
        cm.execute((ConnectionConsumer) mgr::init, 1L);

        int uid = 777;
        var t1 = mgr.generateNew(uid);
        var t2 = mgr.generateNew(uid);

        // Both valid
        assertTrue(mgr.test(t1.token_id(), t1.token()).isPresent());
        assertTrue(mgr.test(t2.token_id(), t2.token()).isPresent());

        // Remove only t1
        mgr.remove(uid, t1.token_id());
        assertTrue(mgr.test(t1.token_id(), t1.token()).isEmpty());
        assertTrue(mgr.test(t2.token_id(), t2.token()).isPresent());

        // Remove remaining tokens for uid
        mgr.remove(uid);
        assertTrue(mgr.test(t2.token_id(), t2.token()).isEmpty());
    }

    @DatabaseTest
    void not_initialized_guard_throws(ConnectionManager cm) {
        var mgr = new SQLSessionTokenUUIDManager(cm, randTable(), Duration.ofMinutes(10).toMillis(), false);

        assertThrows(IllegalArgumentException.class, () -> mgr.generateNew(1));
        assertThrows(IllegalArgumentException.class, () -> mgr.test(UUID.randomUUID(), "abc"));
    }
}
