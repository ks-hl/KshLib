package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provides a number with no input.<br>
 * e.g. rand()
 */
public class NullaryNode extends EquationNode {
    private final Supplier<Double> operator;
    private final String functionName;

    public NullaryNode(Supplier<Double> operator, String functionName, ConstructorParams params) {
        super(params);
        this.operator = operator;
        this.functionName = functionName;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return operator.get();
    }

    @Override
    public String toString(boolean reduce) {
        return functionName + "()";
    }

    @Override
    public boolean isConstant() {
        return false;
    }
}
