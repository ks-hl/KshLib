package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.misc.Formatter;
import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A static value
 */
public class NumberNode extends EquationNode {
    private final double value;

    public NumberNode(double value, ConstructorParams params) {
        super(params);
        this.value = value;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return value;
    }

    @Override
    public String toString(boolean reduce) {
        return Formatter.toString(value, 64, true, true);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
