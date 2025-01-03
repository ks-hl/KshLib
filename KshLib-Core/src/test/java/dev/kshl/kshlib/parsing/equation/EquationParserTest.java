package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.misc.TimeUtil;
import dev.kshl.kshlib.misc.Timer;
import dev.kshl.kshlib.parsing.equation.exception.EvaluationException;
import dev.kshl.kshlib.parsing.equation.exception.LogicException;
import dev.kshl.kshlib.parsing.equation.exception.ParseException;
import dev.kshl.kshlib.parsing.equation.exception.TimeoutException;
import dev.kshl.kshlib.parsing.equation.exception.VariableNotSetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EquationParserTest {

    private static final double DELTA = 1e-10;

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void benchmarkReduced() {
        benchmark("floor(sqrt(5) * 10) + ln(sin(30)) - (log(100) * cos(60)) / pi + e ^ 2 * (1 && 1 || 0 == 1 > 7 < 8) + sum(1,1000,n)", null);
    }

    @Test
    @Timeout(value = 3000, unit = TimeUnit.MILLISECONDS)
    public void benchmarkNonReduced() {
        String eq = "floor(sqrt(5) * 10) + ln(sin(a)) - (log(b) * cos(c)) / pi + e ^ rand() % 2 * (1 && 1 || 0 == 1 > 7 < 8) + sum(1,b,n) + a";
        Map<String, Supplier<Double>> variables = Map.of("a", () -> 3D, "b", () -> 1000D, "c", () -> 100D);
        benchmark(eq, variables);
    }

    private void benchmark(String equation, Map<String, Supplier<Double>> variables) {
        int iterations = 5000;
        Timer timer = new Timer("Parsing");

        for (int i = 0; i < iterations; i++) {
            EquationParser.parse(equation);
        }
        System.out.println(timer + " for " + iterations + ", " + Math.round(timer.getMillis() / iterations * 1E6) / 1E3 + "us/parse");

        EquationNode node = EquationParser.parse(equation);
        System.out.println("Full: " + node);
        System.out.println("Reduced: " + node.toString(true));
        timer = new Timer("Evaluating");
        for (int i = 0; i < iterations; i++) {
            node.evaluate(variables);
        }
        System.out.println(timer + " for " + iterations + ", " + Math.round(timer.getMillis() / iterations * 1E6) / 1E3 + "us/eval");
    }

    private static void benchmark(String title, Runnable action, int iterations) {
        Timer timer = new Timer(title);

        for (int i = 0; i < iterations; i++) {
            action.run();
        }
        System.out.println(timer + " for " + iterations + ", " + TimeUtil.millisToString(timer.getMillis() / iterations) + "/each");
    }

    @Test
    public void testReducedConstant() {
        EquationNode node = EquationParser.parse("floor(sqrt(5) * 10, 2) + ln(sin(30)) - (log(100) * cos(60)) / pi + e ^ 2 * (1 && 1 || 0 == 1 > 7 < 8) + sum(1,1000,n)");
        assertTrue(node.isConstant(), "Node is not constant");
    }

    @Test
    public void testBasicArithmetic() {
        assertEquals(7, EquationParser.parse("3+4").evaluate(), DELTA);
        assertEquals(2, EquationParser.parse("5-3").evaluate(), DELTA);
        assertEquals(15, EquationParser.parse("3*5").evaluate(), DELTA);
        assertEquals(2, EquationParser.parse("6/3").evaluate(), DELTA);
    }

    @Test
    public void testFunctions() {
        assertEquals(Math.sqrt(4), EquationParser.parse("sqrt(4)").evaluate(), DELTA);
        assertEquals(Math.sin(Math.toRadians(30)), EquationParser.parse("sin(30)").evaluate(), DELTA);
        assertEquals(2, EquationParser.parse("if(1,2,3)").evaluate(), DELTA);
        assertEquals(3, EquationParser.parse("if(0,2,3)").evaluate(), DELTA);
        // Add more function tests here
    }

    @Test
    public void testRounding() {
        assertEquals(4, EquationParser.parse("round(3.5)").evaluate(), DELTA);
        assertEquals(4, EquationParser.parse("round(4.1)").evaluate(), DELTA);
        assertEquals(4.1, EquationParser.parse("round(4.11,1)").evaluate(), DELTA);
        assertEquals(3.9, EquationParser.parse("round(3.89999,1)").evaluate(), DELTA);
    }

    @Test
    public void testVariables() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        variables.put("x", () -> 5.0);
        variables.put("y", () -> 3.0);
        assertEquals(8, EquationParser.parse("x+y").evaluate(variables), DELTA);
        assertEquals(15, EquationParser.parse("x*y").evaluate(variables), DELTA);
    }

    @Test
    public void testConstants() {
        assertEquals(Math.PI, EquationParser.parse("pi").evaluate(), DELTA);
        assertEquals(Math.E, EquationParser.parse("e").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("true").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("false").evaluate(), DELTA);
    }

    /**
     * Ensures `pi` and `e` don't conflict with the parsing of variable names starting with those letters
     */
    @Test
    public void testVariableConflictWithConstants() {
        assertEquals(5, EquationParser.parse("pix").evaluate(Map.of("pix", () -> 5D)), DELTA);
        assertEquals(3, EquationParser.parse("ex").evaluate(Map.of("ex", () -> 3D)), DELTA);
    }

    @Test
    public void testInvalidExpression() {
        assertThrows(ParseException.class, () -> EquationParser.parse("3+").evaluate());
    }

    @Test
    public void testAdditionAndMultiplication() {
        assertEquals(2 + 3 * 3, EquationParser.parse("2 + 3 * 3").evaluate(), DELTA);
    }

    @Test
    public void testParenthesesPrecedence() {
        assertEquals((2 + 3) * 4, EquationParser.parse("(2 + 3) * 4").evaluate(), DELTA);
        assertEquals(2 * (3 + 4), EquationParser.parse("2 * (3 + 4)").evaluate(), DELTA);
    }

    @Test
    public void testExponentiationPrecedence() {
        assertEquals(9, EquationParser.parse("3^2").evaluate(), DELTA);
        assertEquals(81, EquationParser.parse("3^2^2").evaluate(), DELTA); // 3^(2^2)
    }

    @Test
    public void testMixedOperations() {
        assertEquals(2 + 3 * 5 - 4D / 2, EquationParser.parse("2 + 3 * 5 - 4 / 2").evaluate(), DELTA);
        assertEquals(8D / 2 + 3 - 1D * 2, EquationParser.parse("8 / 2 + 3 - 1 * 2").evaluate(), DELTA);
    }

    @Test
    public void testMultiplyDivideModuloOrder() {
        assertEquals(3D * 3 / 3, EquationParser.parse("3*3/3").evaluate(), DELTA);
        assertEquals(3D / 3 * 3, EquationParser.parse("3/3*3").evaluate(), DELTA);
        assertEquals(9 % 3D / 3 * 3, EquationParser.parse("9%3/3*3").evaluate(), DELTA);
        assertEquals(3D / 3 * 3 % 2, EquationParser.parse("3/3*3%2").evaluate(), DELTA);
    }

    @Test
    public void testAddSubtractOrder() {
        assertEquals(3 + 3 - 3, EquationParser.parse("3+3-3").evaluate(), DELTA);
        assertEquals(3, EquationParser.parse("3-3+3").evaluate(), DELTA);
    }

    @Test
    public void testUnaryMinus() {
        assertEquals(-3, EquationParser.parse("-3").evaluate(), DELTA);
        assertEquals(4, EquationParser.parse("-2 * -2").evaluate(), DELTA);
        assertEquals(-10, EquationParser.parse("-(2 + 3) * 2").evaluate(), DELTA);
    }

    @Test
    public void testComplexExpressions() {
        assertEquals(32.24, EquationParser.parse("4 * (3 + 5.06)").evaluate(), DELTA);
        assertEquals(-2, EquationParser.parse("2 * (3 - 5) + 2").evaluate(), DELTA);
        assertEquals(10.5, EquationParser.parse("(4.5 + 2.5) * (3 / 2)").evaluate(), DELTA);
    }

    @Test
    public void testLogic() {
        assertEquals(0, EquationParser.parse("0 || 0").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("0 || 1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 || 0").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 || 1").evaluate(), DELTA);

        assertEquals(0, EquationParser.parse("0 && 0").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("0 && 1").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("1 && 0").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 && 1").evaluate(), DELTA);

        assertEquals(1, EquationParser.parse("1 || 0 && 0").evaluate(), DELTA);
    }

    @Test
    public void testComparisons() {
        assertEquals(1, EquationParser.parse("1 >= 1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 <= 1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 = 1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 == 1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 != 1.1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 <> 1.1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1 < 1.1").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("1.2 > 1.1").evaluate(), DELTA);

        assertEquals(0, EquationParser.parse("1.2 < 1.1").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("1.2 <= 1.1").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("1.2 = 1.1").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("1.2 == 1.1").evaluate(), DELTA);
    }

    @Test
    public void testInvalidComparison() {
        assertThrows(ParseException.class, () -> EquationParser.parse("1>").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("1>>2").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("2>>3").evaluate());
    }

    @Test
    public void testInvalidLogic() {
        assertThrows(ParseException.class, () -> EquationParser.parse("1|").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("1||").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("1|=").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("|").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("&").evaluate());
        assertThrows(LogicException.class, () -> EquationParser.parse("1||2").evaluate());
        assertThrows(LogicException.class, () -> EquationParser.parse("1&&2").evaluate());
    }

    @Test
    public void testModuloOperator() {
        assertEquals(1, EquationParser.parse("5 % 2").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("6 % 3").evaluate(), DELTA);
    }

    @Test
    public void testNestedFunctions() {
        assertEquals(Math.floor(Math.sqrt(5) * 10) + Math.log(Math.sin(Math.toRadians(30))), EquationParser.parse("floor(sqrt(5)*10) + ln(sin(30))").evaluate(), DELTA);
        assertEquals(Math.log10(100) * Math.cos(Math.toRadians(60)), EquationParser.parse("log(100) * cos(60)").evaluate(), DELTA);
        assertEquals(4.1, EquationParser.parse("round(floor(4.11111,2),1)").evaluate(), DELTA);
        assertEquals(Math.ceil(Math.sin(Math.toRadians(4.1))), EquationParser.parse("ceil(sin(round(floor(4.11111,2),1)),0)").evaluate(), DELTA);
    }

    @Test
    public void testUnaryMinusWithParentheses() {
        assertEquals(-5, EquationParser.parse("- (3 + 2)").evaluate(), DELTA);
        assertEquals(2, EquationParser.parse("-(3 - 5)").evaluate(), DELTA);
    }

    @Test
    public void testMultipleVariables() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        double x = 5, y = 3, z = 2, z2 = 6, Z3 = 30;
        variables.put("x", () -> x);
        variables.put("y", () -> y);
        variables.put("z", () -> z);
        variables.put("z_2", () -> z2);
        variables.put("Z3", () -> Z3);

        assertEquals(x + y * z, EquationParser.parse("x + y * z").evaluate(variables), DELTA);
        assertEquals((x + y) * z2, EquationParser.parse("(x + y) * z_2").evaluate(variables), DELTA);
        assertEquals((x + y) * Z3, EquationParser.parse("(x + y) * Z3").evaluate(variables), DELTA);
    }

    @Test
    public void testVariableCharacters() {
        // Checks that the variable parses as `z` not `2z`
        assertThrows(VariableNotSetException.class, () -> EquationParser.parse("2z").evaluate(Map.of("2z", () -> 1D)));

        assertThrows(ParseException.class, () -> EquationParser.parse("2_z"));
    }

    @Test
    public void testComplexFunctionsWithVariables() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        double a = 2, b = 3;
        variables.put("a", () -> 2.0);
        variables.put("b", () -> 3.0);

        assertEquals(Math.sqrt(a + a) * Math.sin(Math.toRadians(b)), EquationParser.parse("sqrt(a + a) * sin(b)").evaluate(variables), DELTA);
        assertEquals(Math.log10(a * 50) * Math.cos(Math.toRadians(b)), EquationParser.parse("log(a * 50) * cos(b)").evaluate(variables), DELTA);
    }

    @Test
    public void testComplexLogic() {
        assertEquals(1, EquationParser.parse("(1 || 0) && (1 && 1)").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("(1 || 0) && (0 && 1)").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("(1 == 1) || (2 != 3)").evaluate(), DELTA);
    }

    @Test
    public void testComplexComparisons() {
        assertEquals(1, EquationParser.parse("((1 + 2) * 3) > 6").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("(1 + 2) * 3 < 6").evaluate(), DELTA);
        assertEquals(1, EquationParser.parse("((1 + 2) * 3) >= 6").evaluate(), DELTA);
        assertEquals(0, EquationParser.parse("(1 + 2) * 3 <= 5").evaluate(), DELTA);
    }

    @Test
    public void testExponentiationWithVariables() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        variables.put("a", () -> 2.0);

        assertEquals(Math.pow(2, 3), EquationParser.parse("a ^ 3").evaluate(variables), DELTA);
        assertEquals(Math.pow(2, Math.sqrt(4)), EquationParser.parse("a ^ sqrt(4)").evaluate(variables), DELTA);
    }

    @Test
    public void testComplexExpressionWithAllOperators() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        double x = 5, y = 3;
        variables.put("x", () -> x);
        variables.put("y", () -> y);

        assertEquals((2 + x) * (y - 1), EquationParser.parse("(2 + x) * (y - 1)").evaluate(variables), DELTA);
        assertEquals(((x + y) * (x + y)) / (x - y), EquationParser.parse("((x + y) ^ 2) / (x - y)").evaluate(variables), DELTA);
    }

    @Test
    public void testComplexExpressionWithFunctionsAndVariables() {
        Map<String, Supplier<Double>> variables = new HashMap<>();
        variables.put("a", () -> 2.0);
        variables.put("b", () -> 3.0);

        assertEquals(Math.sqrt(4) * Math.sin(Math.toRadians(3)) + (Math.log10(100) * Math.cos(Math.toRadians(3))), EquationParser.parse("sqrt(a + a) * sin(b) + log(a * 50) * cos(b)").evaluate(variables), DELTA);
    }

    @Test
    public void testInvalidExpressionWithMissingParentheses() {
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("(3 + 2").evaluate());
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("3 + (2 - 1").evaluate());
    }

    @Test
    public void testInvalidExpressionWithExtraParentheses() {
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("((3 + 2)").evaluate());
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("(3 + 2))").evaluate());
    }

    @Test
    public void testInvalidExpressionWithMissingOperator() {
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("3 2").evaluate());
        assertThrows(IllegalArgumentException.class, () -> EquationParser.parse("3+ 2*").evaluate());
    }

    @Test
    public void testInvalidExpressionWithUnknownFunction() {
        assertThrows(ParseException.class, () -> EquationParser.parse("unknown(3)").evaluate());
        assertThrows(ParseException.class, () -> EquationParser.parse("sin(3, 4)").evaluate());
    }

    @Test
    public void testMissingVariable() {
        assertThrows(VariableNotSetException.class, () -> EquationParser.parse("a").evaluate());
        assertThrows(VariableNotSetException.class, () -> EquationParser.parse("a").evaluate(Map.of("b", () -> 1d)));
    }

    @Test
    public void testRand() {
        for (int i = 0; i < 10000; i++) {
            double val = EquationParser.parse("rand()").evaluate();
            assert val >= 0d;
            assert val < 1d;
        }
    }

    @Test
    public void testSummation() {
        assertEquals(10, EquationParser.parse("sum(1,10,1)").evaluate());
        assertEquals(6, EquationParser.parse("sum(1,3,n)").evaluate());
        assertEquals(6, EquationParser.parse("sum(1+1-1,3/3*3,n)").evaluate());
    }

    @Test
    public void testNestedSummation() {
        assertEquals(18, EquationParser.parse("sum(1,3,sum(1,3,n))").evaluate());
        assertEquals(36, EquationParser.parse("sum(1,3,n*sum(1,3,n))").evaluate());
    }

    @Test
    public void testInvalidSummation() {
        assertThrows(EvaluationException.class, () -> EquationParser.parse("sum(10,1,1)"));
    }

    @Test
    public void testToString() {
        String equation = "floor(sqrt(5)*10)+ln(sin(30))-(log(100)*cos(60))/pi+e^rand()%2*(1&&1||0==1>7<8999999999999999)+x+y+-z";
        EquationNode node = EquationParser.parse(equation);
        assertEquals(equation, node.toString());
        assertEquals("20.988542933256262+2.718281828459045^rand()%2*1+x+y+-z", node.toString(true));
        System.out.println(equation);
        System.out.println(node.toString(false));
        System.out.println(node.toString(true));
    }

    @Test
    @Timeout(value = 1200, unit = TimeUnit.MILLISECONDS)
    public void testTimeout() {
        Supplier<TimeoutManager> timeoutManagerSupplier = () -> new TimeoutManager(50, TimeUnit.MILLISECONDS);
        // Test evaluation timeout
        // Using X prevents reduction/solve during parse
        assertThrows(TimeoutException.class, () -> EquationParser.parse("sum(1,1000000000,sqrt(n*x))").evaluate(Map.of("x", () -> 1D), timeoutManagerSupplier.get(), 1000, 0));

        // Test parsing reduction timeout
        StringBuilder equation = new StringBuilder("n");
        for (int i = 0; i < 16; i++) {
            equation = new StringBuilder("sum(1,10000000," + equation + ")");
        }
        String eq = equation.toString();
        assertThrows(TimeoutException.class, () -> EquationParser.builder().timeout(timeoutManagerSupplier.get()).setSummationLoopLimit(0).parse(eq));

        // Test parsing timeout
        String longEq = "pi+".repeat(100).repeat(100).repeat(100).repeat(100) + "1";
        assertThrows(TimeoutException.class, () -> EquationParser.builder().timeout(timeoutManagerSupplier.get()).parse(longEq));
    }

    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    public void testInterrupt() {
        Thread thread = Thread.currentThread();
        Executors.newSingleThreadScheduledExecutor().schedule(thread::interrupt, 5, TimeUnit.MILLISECONDS);
        assertThrows(TimeoutException.class, () -> EquationParser.builder().setSummationLoopLimit(0).parse("sum(1,1000000000,sqrt(n))"));
    }

    @Test
    public void testInvalidOperations() {
        assertThrows(EvaluationException.class, () -> EquationParser.parse("1/0").evaluate());
        assertThrows(EvaluationException.class, () -> EquationParser.parse("log(0)").evaluate());
        assertThrows(EvaluationException.class, () -> EquationParser.parse("log(-1)").evaluate());
        assertThrows(EvaluationException.class, () -> EquationParser.parse("ln(0)").evaluate());
        assertThrows(EvaluationException.class, () -> EquationParser.parse("ln(-1)").evaluate());
        assertThrows(EvaluationException.class, () -> EquationParser.parse("sqrt(-1)").evaluate());
    }

    @Test
    public void testEmptyEquation() {
        assertThrows(ParseException.class, () -> EquationParser.parse(""));
    }

    @Test
    public void testImpliedMultiplication() {
        assertEquals(10, EquationParser.parse("5(2)").evaluate());
        assertEquals(10, EquationParser.parse("5a").evaluate(Map.of("a", () -> 2D)));
    }
}
