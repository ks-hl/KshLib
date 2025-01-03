package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Performs an action on a single node.
 */
public class TrinaryNode extends EquationNode {
    private final Operator operator;
    protected final EquationNode a;
    protected final EquationNode b;
    protected final EquationNode c;
    private final String functionName;

    public TrinaryNode(Operator operator, EquationNode a, EquationNode b, EquationNode c, String functionName, ConstructorParams params) {
        super(params);
        this.operator = operator;
        this.a = a.reduce(params);
        this.b = b.reduce(params);
        this.c = c.reduce(params);
        this.functionName = functionName;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return operator.apply(a.evaluate(params), b.evaluate(params), c.evaluate(params));
    }

    @Override
    public String toString(boolean reduce) {
        return String.format("%s(%s,%s,%s)", functionName, a.toString(reduce), b.toString(reduce), c.toString(reduce));
    }

    @Override
    public boolean isConstant() {
        return a.isConstant() && b.isConstant() && c.isConstant();
    }

    @FunctionalInterface
    public interface Operator {
        double apply(double a, double b, double c);
    }
}
