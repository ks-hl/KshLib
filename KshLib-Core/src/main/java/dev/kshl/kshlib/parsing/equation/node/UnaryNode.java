package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Performs an action on a single node.
 */
public class UnaryNode extends EquationNode {
    private final UnaryOperator<Double> operator;
    protected final EquationNode child;
    protected final String functionName;

    public UnaryNode(UnaryOperator<Double> operator, EquationNode child, String functionName, ConstructorParams params) {
        super(params);
        this.operator = operator;
        this.child = child.reduce(params);
        this.functionName = functionName;
    }

    public UnaryNode(UnaryOperator<Double> operator, EquationNode child, ConstructorParams params) {
        this(operator, child, null, params);
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return operator.apply(child.evaluate(params));
    }

    @Override
    public String toString(boolean reduce) {
        if (functionName == null) return child.toString(reduce);
        return functionName + "(" + child.toString(reduce) + ")";
    }

    @Override
    public boolean isConstant() {
        return child.isConstant();
    }
}
