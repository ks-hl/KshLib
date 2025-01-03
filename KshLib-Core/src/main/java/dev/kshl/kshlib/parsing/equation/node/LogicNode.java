package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;

import java.util.function.BinaryOperator;

/**
 * Performs a logic action on two nodes.<br>
 * e.g. AND, OR, etc.
 */
public class LogicNode extends ComparisonNode {
    protected final BinaryOperator<Boolean> operator;

    public LogicNode(BinaryOperator<Boolean> operator, EquationNode left, EquationNode right, String symbol, ConstructorParams params) {
        super(left, right, symbol, params);
        this.operator = operator;
    }

    @Override
    public double evaluate(EvaluationParams params) {
        return toDouble(operator.apply(toBoolean(left.evaluate(params)), toBoolean(right.evaluate(params))));
    }
}
