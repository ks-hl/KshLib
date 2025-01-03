package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;

import java.util.function.BinaryOperator;

/**
 * Performs an action on two nodes
 */
public class OperatorNode extends BinaryNode {

    public OperatorNode(BinaryOperator<Double> operator, EquationNode left, EquationNode right, String operatorSymbol, ConstructorParams params) {
        super(operator, left, right, operatorSymbol, params);
    }

    @Override
    public String toString(boolean reduce) {
        return left.toString(reduce) + functionName + right.toString(reduce);
    }
}
