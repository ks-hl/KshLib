package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilTest {
    @Test
    public void testWriteReadHash() throws IOException {
        String content = "Contented get distrusts certainty nay are frankness concealed ham. On unaffected resolution on considered of. Shot what able cold new the see hold. Friendly as an betrayed formerly he. Morning because as to society behaved moments. Put ladies design mrs sister was. Play on hill felt john no gate. Am passed figure to marked in. Prosperous middletons is ye inhabiting as assistance me especially. For looking two cousins regular amongst.";
        File file = new File("test/test.txt");
        boolean ignored = file.delete();
        FileUtil.write(file, content);
        assertEquals(content, FileUtil.read(file));
        assertEquals("9b7a89a068576f5d676de2f840c0b880dda885884178753819240a5176cdadd3", FileUtil.getSHA256HashHex(file));
    }

    @Test
    public void testDelete() throws IOException {
        File file = new File("test/test1/test2/file.txt");
        boolean ignored = file.getParentFile().mkdirs();
        FileUtil.write(file, "");
        assertTrue(file.exists());

        FileUtil.delete(new File("test"));
        assertFalse(file.exists());
    }
}
