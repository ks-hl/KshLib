package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.encryption.CodeGenerator;
import dev.kshl.kshlib.encryption.HashPBKDF2;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.Timer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SQLPasswordManagerTest {
    private static Stream<SQLPasswordManager.Type> getTypes() {
        return Stream.of(SQLPasswordManager.Type.values());
    }

    @ParameterizedTest
    @MethodSource("getTypes")
    public void testHashing(SQLPasswordManager.Type type) throws NoSuchAlgorithmException {
        Set<String> passwords = new HashSet<>();
        Set<byte[]> hashes = new HashSet<>();

        for (int i1 = 0; i1 < 3; i1++) {
            Timer timer = new Timer();
            String password = CodeGenerator.generateSecret(i1 * 6 + 8, true, true, true);
            byte[] hash = HashPBKDF2.hash(password, type.iterationCount);
            assert !Arrays.equals(hash, HashPBKDF2.hash(password, type.iterationCount));
            System.out.printf("Iterations: %s\nPassword: %s\n  Hash: %s\n  Elapsed: %s\n", type.iterationCount, password, Base64.getEncoder().encodeToString(hash), timer);
            //noinspection AssertWithSideEffects
            assert passwords.add(password);
            //noinspection AssertWithSideEffects
            assert hashes.add(hash);

            assert HashPBKDF2.test(hash, password, type.iterationCount);
            assert !HashPBKDF2.test(hash, password + " ", type.iterationCount);
        }
    }

    @DatabaseTest
    public void testSQLPasswordManager(ConnectionManager connectionManager) throws SQLException, BusyException, NoSuchAlgorithmException {
        connectionManager.execute("DROP TABLE IF EXISTS passwords", 100);
        SQLPasswordManager sqlPasswordManager = new SQLPasswordManager(connectionManager, "passwords", SQLPasswordManager.Type.TOKEN);
        connectionManager.execute(sqlPasswordManager::init, 100);

        // setPassword
        final long setTime = System.currentTimeMillis();
        sqlPasswordManager.setPassword(1, "password1", true, 0);
        sqlPasswordManager.setPassword(2, "password2", true, 1); // expired
        // setPassword (require new)
        assertThrows(SQLException.class, () -> sqlPasswordManager.setPassword(1, "password1", true, 0));
        // testPassword
        assert sqlPasswordManager.testPassword(1, "password1");
        assert !sqlPasswordManager.testPassword(1, "fjiwugneskj");
        assert !sqlPasswordManager.testPassword(2, "password1");
        assert !sqlPasswordManager.testPassword(1, "password2");
        // testPassword (expired)
        assert !sqlPasswordManager.testPassword(2, "password2");
        // contains
        assert sqlPasswordManager.contains(1);
        assert sqlPasswordManager.contains(2);
        // listUsers
        Set<Integer> uids = sqlPasswordManager.listUsers();
        assert uids.contains(1);
        assert uids.contains(2);
        // getLastChanged
        assertEquals(setTime, sqlPasswordManager.getLastChanged(1), 3);
        // remove
        sqlPasswordManager.remove(1);
        assert !sqlPasswordManager.contains(1);
    }
}
