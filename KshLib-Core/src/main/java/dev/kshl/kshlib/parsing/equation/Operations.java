package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.misc.FloatingMath;
import dev.kshl.kshlib.parsing.equation.exception.EvaluationException;

import java.util.function.BinaryOperator;

public class Operations {
    public static final BinaryOperator<Double> DIVISION = (l, r) -> {
        if (r == 0) {
            throw new EvaluationException("Divide by 0");
        }
        return EquationNode.checkFinite(l / r);
    };
    public static final BinaryOperator<Double> FLOOR = (l, r) -> FloatingMath.withSignificance(l, Math::floor, (int) Math.round(r));
    public static final BinaryOperator<Double> CEILING = (l, r) -> FloatingMath.withSignificance(l, Math::ceil, (int) Math.round(r));
    public static final BinaryOperator<Double> ROUND = (l, r) -> FloatingMath.withSignificance(l, d -> (double) Math.round(d), (int) Math.round(r));
}
