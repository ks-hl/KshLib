package dev.kshl.kshlib.parsing.equation.exception;

import dev.kshl.kshlib.parsing.equation.node.VariableNode;

public class VariableNotSetException extends EvaluationException {
    private final VariableNode variableNode;

    public VariableNotSetException(VariableNode variableNode) {
        super("Variable '" + variableNode + "' not set.");
        this.variableNode = variableNode;
    }

    public VariableNode getVariableNode() {
        return variableNode;
    }
}
