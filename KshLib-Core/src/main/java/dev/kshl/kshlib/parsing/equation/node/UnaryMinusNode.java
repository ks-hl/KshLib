package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.TimeoutManager;

import java.util.function.UnaryOperator;

/**
 * Performs an action on a single node.
 */
public class UnaryMinusNode extends UnaryNode {

    public UnaryMinusNode(UnaryOperator<Double> operator, EquationNode child, ConstructorParams params) {
        super(operator, child, "-", params);
    }

    @Override
    public String toString(boolean reduce) {
        return functionName + child.toString(reduce);
    }
}
