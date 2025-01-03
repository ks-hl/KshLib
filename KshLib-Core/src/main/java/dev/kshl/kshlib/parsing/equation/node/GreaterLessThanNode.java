package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Compares the value of two nodes based on the provided booleans lessThan, equals, and greaterThan.
 */
public class GreaterLessThanNode extends ComparisonNode {
    private final boolean lessThan, equals, greaterThan;

    public GreaterLessThanNode(boolean lessThan, boolean equals, boolean greaterThan, EquationNode left, EquationNode right, String symbol, ConstructorParams params) {
        super(left, right, symbol, params);

        this.lessThan = lessThan;
        this.equals = equals;
        this.greaterThan = greaterThan;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        final double l = left.evaluate(params);
        final double r = right.evaluate(params);
        final double rLow = r - TOLERANCE;
        final double rHigh = r + TOLERANCE;

        if (lessThan && l < rLow) return 1;
        if (equals && l <= rHigh && l >= rLow) return 1;
        if (greaterThan && l > rHigh) return 1;

        return 0;
    }
}
