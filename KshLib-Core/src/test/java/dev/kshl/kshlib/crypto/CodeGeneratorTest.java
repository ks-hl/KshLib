package dev.kshl.kshlib.crypto;

import dev.kshl.kshlib.misc.StringUtil;
import org.junit.jupiter.api.Test;

public class CodeGeneratorTest {
    @Test
    public void testCodeGeneration() {
        testCode(8, true, true, true);

        testCode(100, true, false, false);
        testCode(101, false, true, false);
        testCode(102, false, false, true);
    }

    private void testCode(int len, boolean upper, boolean numbers, boolean special) {
        String code = CodeGenerator.generateSecret(len, upper, numbers, special);

        assert code.length() == len;
        assert !upper || StringUtil.containsAnyOf(code, CodeGenerator.LETTERS.toUpperCase());
        assert !numbers || StringUtil.containsAnyOf(code, CodeGenerator.NUMBERS);
        assert !special || StringUtil.containsAnyOf(code, CodeGenerator.SPECIAL);
    }
}
