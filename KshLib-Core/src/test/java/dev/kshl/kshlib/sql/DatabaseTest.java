package dev.kshl.kshlib.sql;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ParameterizedTest
@MethodSource("dev.kshl.kshlib.sql.DatabaseManagerTest#provideConnectionManagers")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@TestTemplate
@Testable
public @interface DatabaseTest {
}
