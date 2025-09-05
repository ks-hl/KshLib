package dev.kshl.kshlib.crypto;

import dev.kshl.kshlib.misc.StringUtil;

import java.security.SecureRandom;

public class CodeGenerator {

    public static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMBERS = "0123456789";
    public static final String SPECIAL = "!@#$%^&*()-=_+[]{};:\\|,./<>?`~";
    public static final String PRODUCT_KEY_CHARACTERS = "2346789BCDFGHJKMPQRTVWXY";

    /**
     * Generates a product key containing only the characters '2346789BCDFGHJKMPQRTVWXY', with lengths as specified
     *
     * @param lengths An array of ints where each element is the length of one of the key's parts. For instance, [1,2,3] would output a key formatted as X-XX-XXX
     * @return the key
     */
    public static String generateProductKey(int... lengths) {
        if (lengths.length == 0) throw new IllegalArgumentException("format must not be empty");
        SecureRandom random = new SecureRandom();

        StringBuilder code = new StringBuilder();
        for (int len : lengths) {
            if (len <= 0) throw new IllegalArgumentException("all lens must be > 0");
            if (!code.isEmpty()) code.append("-");
            code.append(StringUtil.repeat(() -> StringUtil.randomCharFrom(random, PRODUCT_KEY_CHARACTERS), len));
        }
        return code.toString();
    }

    /**
     * @see CodeGenerator#generateSecret(int, boolean, boolean, boolean, boolean)
     */
    public static String generateSecret(int length, boolean upper, boolean numbers, boolean special) {
        return generateSecret(length, upper, numbers, special, true);
    }

    /**
     * Generates a secure secret code with the specified parameters.
     *
     * @param length           The length of the returned secret
     * @param upper            Whether to include upper-case letters (+26 characters)
     * @param numbers          Whether to include numbers (+10 characters)
     * @param special          Whether to include special characters (+30 characters)
     * @param requireSpecified Whether to require that the returned password contains at least one of each of the specified parameters.
     * @return The generated secret
     */
    public static String generateSecret(int length, boolean upper, boolean numbers, boolean special, boolean requireSpecified) {
        if (length < 8 && !requireSpecified) throw new IllegalArgumentException("length must be >= 8");
        if (length > 1000) throw new IllegalArgumentException("length must be <= 1000");
        SecureRandom random = new SecureRandom();
        String seed_ = LETTERS;
        if (upper) seed_ += LETTERS.toUpperCase();
        if (numbers) seed_ += NUMBERS;
        if (special) seed_ += SPECIAL;
        final String seed = seed_;

        String out = null;
        for (int i = 0; i < 1000; i++) {
            out = StringUtil.repeat(() -> StringUtil.randomCharFrom(random, seed), length);

            if (requireSpecified) {
                if (upper && !StringUtil.containsAnyOf(out, LETTERS.toUpperCase())) continue;
                if (numbers && !StringUtil.containsAnyOf(out, NUMBERS)) continue;
                if (special && !StringUtil.containsAnyOf(out, SPECIAL.toUpperCase())) continue;
            }

            break;
        }
        return out;
    }
}
