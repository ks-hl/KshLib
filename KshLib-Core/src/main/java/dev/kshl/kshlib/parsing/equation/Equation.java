package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.parsing.equation.exception.OperationLimitExceeded;
import dev.kshl.kshlib.parsing.equation.exception.ParseException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Equation {
    private final EquationNode node;
    private final Map<String, Supplier<Double>> variables = new HashMap<>();
    private final Map<String, AtomicInteger> variableHitCount = new HashMap<>();
    private static final int RECURSION_LIMIT = 500;

    /**
     * Converts a String to an equation which can be programmatically evaluated.<br><br>
     * <p>
     * Valid operators include (in order of operations):<br>
     * <table>
     *   <tr><td>()</td> <td>Grouping parenthesis</td></tr>
     *   <tr><td>^</td> <td>Exponentiation</td></tr>
     *   <tr><td>*</td> <td>Multiplication</td></tr>
     *   <tr><td>/</td> <td>Division</td></tr>
     *   <tr><td>%</td> <td>Modulo</td></tr>
     *   <tr><td>+</td> <td>Addition</td></tr>
     *   <tr><td>-</td> <td>Subtraction</td></tr>
     *   <tr><td>&&</td> <td>AND</td></tr>
     *   <tr><td>||</td> <td>OR</td></tr>
     *   <tr><td>==</td> <td>Equals</td></tr>
     *   <tr><td>!=</td> <td>Not Equals</td></tr>
     *   <tr><td>></td> <td>Greater Than</td></tr>
     *   <tr><td>>=</td> <td>Greater Than Or Equals</td></tr>
     *   <tr><td><</td> <td>Less Than</td></tr>
     *   <tr><td><=</td> <td>Less Than Or Equals</td></tr>
     * </table>
     *
     * @param equation The equation to be parsed
     * @throws ParseException if the syntax of the equation is invalid or unknown operators or functions are used.
     */
    public Equation(String equation) {
        this(EquationParser.parse(equation));
    }

    Equation(EquationNode node) {
        this.node = node;
    }

    public void setVariable(String variable, EquationNode value) {
        setVariable(variable, () -> {
            if (variableHitCount.computeIfAbsent(variable, v -> new AtomicInteger()).incrementAndGet() >= RECURSION_LIMIT) {
                throw new OperationLimitExceeded("Variable '" + variable + "' has cyclic dependencies.");
            }
            return value.evaluate(variables);
        });
    }

    public void setVariable(String variable, double value) {
        setVariable(variable, () -> value);
    }

    public void setVariable(String variable, Supplier<Double> value) {
        variables.put(variable, value);
    }

    public double evaluate() {
        variableHitCount.clear();
        return node.evaluate(variables);
    }
}
