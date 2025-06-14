package dev.kshl.kshlib.parsing;

import dev.kshl.kshlib.function.ThrowingSupplier;
import dev.kshl.kshlib.misc.UUIDHelper;

import java.util.Optional;
import java.util.UUID;

public class OptionalParser {
    public static <T> Optional<T> get(ThrowingSupplier<T, IllegalArgumentException> throwingSupplier) {
        try {
            return Optional.ofNullable(throwingSupplier.get());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseLong(String string) {
        return get(() -> Long.parseLong(string));
    }

    public static Optional<Integer> parseInt(String string) {
        return get(() -> Integer.parseInt(string));
    }

    public static Optional<Boolean> parseBoolean(String string) {
        return get(() -> switch (string) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> throw new IllegalArgumentException();
        });
    }

    public static Optional<Double> parseDouble(String string) {
        return get(() -> Double.parseDouble(string));
    }

    public static Optional<Float> parseFloat(String string) {
        return get(() -> Float.parseFloat(string));
    }

    public static Optional<Short> parseShort(String string) {
        return get(() -> Short.parseShort(string));
    }

    public static Optional<Byte> parseByte(String string) {
        return get(() -> Byte.parseByte(string));
    }

    public static Optional<UUID> parseUUID(String string) {
        return get(() -> UUIDHelper.fromString(string));
    }
}
