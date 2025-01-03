package dev.kshl.kshlib.net;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUUIDAPI {
    @Test
    @Timeout(value = 5000L)
    public void testAPIs() {
        UUIDAPI2 uuidAPI = new UUIDAPI2();
        BiConsumer<String, UUID> testGetName = (actualName, actualUUID) -> {
            try {
                Timer timer = new Timer("Name request (" + actualName + ")");
                String name = uuidAPI.getUsernameFromUUID(actualUUID);
                assertEquals(actualName, name);
                System.out.println(name);
                System.out.println(timer);
            } catch (IOException | BusyException e) {
                throw new RuntimeException(e);
            }
        };
        BiConsumer<String, UUID> testGetUUID = (actualName, actualUUID) -> {
            try {
                Timer timer = new Timer("UUID request (" + actualUUID + ")");
                UUID uuid = uuidAPI.getUUIDFromUsername(actualName);

                assertEquals(actualUUID, uuid);
                System.out.println(uuid);
                System.out.println(timer);
            } catch (IOException | BusyException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < 2; i++) {
            testGetName.accept(".Heliosaltes", UUID.fromString("00000000-0000-0000-0009-01f8e0ee602f"));
//            testGetUUID.accept(".Heliosares5187", UUID.fromString("00000000-0000-0000-0009-0000057e2dae"));
            testGetName.accept("Heliosares", UUID.fromString("4f159a36-2b22-4fea-8b4e-fedb85c3d61a"));
        }
    }
}
