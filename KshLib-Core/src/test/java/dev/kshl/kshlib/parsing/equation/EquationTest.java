package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.parsing.equation.exception.OperationLimitExceeded;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EquationTest {

    @Test
    public void testRecursionLimit() {
        Equation equation = new Equation(EquationParser.parse("a+b"));
        equation.setVariable("a", EquationParser.parse("b"));
        equation.setVariable("b", EquationParser.parse("a"));

        assertThrows(OperationLimitExceeded.class, equation::evaluate);
    }
}
