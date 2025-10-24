package dev.kshl.kshlib.yaml;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record YamlResult<E>(String key, Optional<E> value, Optional<Object> rawValue, String expectedType) {
    final static String MISSING_MESSAGE = "Missing '%key%'";
    final static String WRONG_TYPE_MESSAGE = "'%key%' is wrong type. Expected '%expected%', is '%actual%'";

    /**
     * Gets the present value, or throws an {@link IllegalArgumentException} with the provided message. Messages can include `%key%` which will be replaced with the object's key.
     *
     * @param messageIfWrongType Can also include %expected% and %actual% to represent the type of the actual/expected value.
     */
    public E orElseThrow(String messageIfNotPresent, String messageIfWrongType) throws IllegalArgumentException {
        if (!isRawPresent()) {
            throw new IllegalArgumentException(formatError(messageIfNotPresent));
        }
        if (!isCorrectType()) {
            throw new IllegalArgumentException(formatError(messageIfWrongType));
        }
        return value.orElseThrow();
    }

    String formatError(String format) {
        format = format.replace("%key%", key());

        if (expectedType() != null) format = format.replace("%expected%", expectedType());
        else format = format.replace("%expected%", "Unknown");

        if (rawValue().isPresent()) format = format.replace("%actual%", rawValue().get().getClass().getName());
        else format = format.replace("%actual%", "Unknown");

        return format;
    }

    public E orElseThrow() throws IllegalArgumentException {
        return orElseThrow(MISSING_MESSAGE, WRONG_TYPE_MESSAGE);
    }

    /**
     * @return Whether the value is present at all (May or may not be of the correct type)
     */
    public boolean isRawPresent() {
        return rawValue().isPresent();
    }

    /**
     * @return Whether the value is present and of the correct type
     */
    public boolean isCorrectType() {
        return value().isPresent();
    }

    public static <E> YamlResult<E> empty(String key, String expectedType) {
        return new YamlResult<>(key, Optional.empty(), Optional.empty(), expectedType);
    }

    public E orElseGet(Supplier<E> supplier) {
        return value.orElseGet(supplier);
    }

    public <T> YamlResult<T> map(Function<E, T> mapper, String expectedType) {
        return new YamlResult<>(key(), value().map(mapper), rawValue(), expectedType);
    }
    public <T> YamlResult<T> flatMap(Function<E, Optional<T>> mapper, String expectedType) {
        return new YamlResult<>(key(), value().flatMap(mapper), rawValue(), expectedType);
    }
}
