package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;

import java.util.function.BinaryOperator;

/**
 * Performs an action on a single node.
 */
public class BinaryNode extends EquationNode {
    private final BinaryOperator<Double> operator;
    protected final EquationNode left;
    protected final EquationNode right;
    protected final String functionName;

    public BinaryNode(BinaryOperator<Double> operator, EquationNode left, EquationNode right, String functionName, ConstructorParams params) {
        super(params);
        this.operator = operator;
        this.left = left.reduce(params);
        this.right = right.reduce(params);
        this.functionName = functionName;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return operator.apply(left.evaluate(params), right.evaluate(params));
    }

    @Override
    public String toString(boolean reduce) {
        return String.format("%s(%s,%s)", functionName, left.toString(reduce), right.toString(reduce));
    }

    @Override
    public boolean isConstant() {
        return left.isConstant() && right.isConstant();
    }
}
