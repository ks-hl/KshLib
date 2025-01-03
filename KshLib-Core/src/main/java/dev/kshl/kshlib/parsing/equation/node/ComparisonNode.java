package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;

/**
 * Compares the value of two nodes based on the provided booleans lessThan, equals, and greaterThan.
 */
public abstract class ComparisonNode extends EquationNode {
    protected final EquationNode left;
    protected final EquationNode right;
    private final String symbol;

    public ComparisonNode(EquationNode left, EquationNode right, String symbol, ConstructorParams params) {
        super(params);
        this.left = left.reduce(params);
        this.right = right.reduce(params);
        this.symbol = symbol;
    }

    @Override
    public String toString(boolean reduce) {
        return left.toString(reduce) + symbol + right.toString(reduce);
    }

    @Override
    public boolean isConstant() {
        return left.isConstant() && right.isConstant();
    }
}
