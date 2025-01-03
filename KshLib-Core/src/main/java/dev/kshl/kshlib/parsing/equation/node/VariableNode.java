package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.exception.EvaluationException;
import dev.kshl.kshlib.parsing.equation.exception.VariableNotSetException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A variable, must be provided by the variables map when evaluating.
 */
public class VariableNode extends EquationNode {

    private final String name;

    public VariableNode(String variableName, ConstructorParams params) {
        super(params);
        this.name = variableName;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        if (params.variables() == null || !params.variables().containsKey(name)) {
            throw new VariableNotSetException(this);
        }
        if (params.variableHitCount().computeIfAbsent(name, n -> new AtomicInteger()).incrementAndGet() > params.recursiveVariableLimit()) {
            throw new EvaluationException("Variable '" + name + "' is recursive.");
        }
        return params.variables().get(name).get();
    }

    @Override
    public String toString(boolean reduce) {
        return isN() ? "n" : name;
    }

    @Override
    public boolean isConstant() {
        return isN(); // Can't be constant because variables are not known during parsing. `n` is not "known", but it is constant in the scope of a single evaluation
    }

    public boolean isN() {
        return name.equals("n") && insideSummation;
    }

    public String getName() {
        return name;
    }

    public static void validateRecursiveLimit(int recursiveVariableLimit) throws IllegalArgumentException {
        if (recursiveVariableLimit < 2) {
            throw new IllegalArgumentException("Recursive limit must be >1");
        }
        if (recursiveVariableLimit > 1000) {
            throw new IllegalArgumentException("Recursive limit must be <=1000");
        }
    }
}
