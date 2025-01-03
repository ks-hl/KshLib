package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.parsing.equation.exception.EvaluationException;
import dev.kshl.kshlib.parsing.equation.exception.LogicException;
import dev.kshl.kshlib.parsing.equation.exception.VariableNotSetException;
import dev.kshl.kshlib.parsing.equation.node.ConstantNode;
import dev.kshl.kshlib.parsing.equation.node.VariableNode;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A portion of or entire equation.
 *
 * @see EquationParser#parse(String)
 */
public abstract class EquationNode {
    /**
     * The tolerance to consider two doubles equivalent.
     */
    public static final double TOLERANCE = 1E-10;
    protected final boolean insideSummation;

    /**
     * Converts a double to its equivalent boolean value
     *
     * @param d The value to evaluate, must be 0 or 1.
     * @return true if d==1, false if d==0
     * @throws LogicException if d!=1 and d!=0
     */
    public static boolean toBoolean(double d) {
        if (Math.abs(d) < TOLERANCE) return false;
        else if (Math.abs(d - 1) < TOLERANCE) return true;
        throw new LogicException("Can not perform logic operations on values other than 1 or 0. (" + d + ")");
    }

    /**
     * Converts a boolean to its equivalent numerical value
     *
     * @param b The state
     * @return 1 if b is true, else 0
     */
    public static double toDouble(boolean b) {
        return b ? 1 : 0;
    }

    public EquationNode(ConstructorParams constructorParams) {
        this.insideSummation = constructorParams.isInSummation();
    }

    /**
     * Solves the underlying equation, substituting the provided variables.
     * If extra variables are provided within the map, they are ignored.
     *
     * @param variables The variables to assign within the equation
     * @return The solved value of the underlying equation
     * @throws VariableNotSetException If the required variables are not provided
     */
    public double evaluate(@Nullable Map<String, Supplier<Double>> variables) {
        return evaluate(variables, null);
    }

    public double evaluate(@Nonnull TimeoutManager timeoutManager) {
        return evaluate(null, timeoutManager);
    }

    public final double evaluate(@Nullable Map<String, Supplier<Double>> variables, @Nullable TimeoutManager timeoutManager) {
        return evaluate(variables, timeoutManager, 1000, 100000);
    }

    public final double evaluate(@Nullable Map<String, Supplier<Double>> variables, @Nullable TimeoutManager timeoutManager, int recursiveVariableLimit, int summationLoopLimit) {
        VariableNode.validateRecursiveLimit(recursiveVariableLimit);
        if (timeoutManager == null) timeoutManager = new TimeoutManager(5, TimeUnit.SECONDS);
        return evaluate(new EvaluationParams(variables, new HashMap<>(), timeoutManager, recursiveVariableLimit, summationLoopLimit));
    }

    public abstract double evaluate(EvaluationParams params);

    /**
     * Equivalent to {@link #evaluate(Map, TimeoutManager)} with no variables (null)
     *
     * @return The solved value of the underlying equation
     * @throws VariableNotSetException If the equation requires variables
     */
    public double evaluate() {
        return evaluate((Map<String, Supplier<Double>>) null);
    }

    public abstract boolean isConstant();

    public EquationNode reduce(ConstructorParams params) {
        if (!isConstant()) return this;
        if (params.reduceEvaluationParams() == null) return this;
        try {
            return new ConstantNode(evaluate(params.reduceEvaluationParams()), this::toString, params);
        } catch (VariableNotSetException e) {
            if (e.getVariableNode().isN()) return this;
            throw e;
        }
    }

    protected static double checkFinite(double val) {
        if (Double.isInfinite(val)) throw new EvaluationException("Infinity");
        if (Double.isNaN(val)) throw new EvaluationException("NaN");
        return val;
    }

    public abstract String toString(boolean reduce);

    @Override
    public final String toString() {
        return toString(false);
    }

    public record ConstructorParams(TimeoutManager timeoutManager, boolean isInSummation, @Nullable EvaluationParams reduceEvaluationParams) {
    }

    public record EvaluationParams(Map<String, Supplier<Double>> variables, Map<String, AtomicInteger> variableHitCount, TimeoutManager timeoutManager, int recursiveVariableLimit, int summationLoopLimit) {
    }
}
