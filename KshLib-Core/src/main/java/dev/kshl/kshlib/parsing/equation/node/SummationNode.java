package dev.kshl.kshlib.parsing.equation.node;

import dev.kshl.kshlib.parsing.equation.EquationNode;
import dev.kshl.kshlib.parsing.equation.exception.EvaluationException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Performs summation
 */
public class SummationNode extends TrinaryNode {

    public SummationNode(EquationNode from, EquationNode to, EquationNode equation, ConstructorParams params) {
        super(null, from, to, equation, "sum", params);
    }

    @Override
    public double evaluate(EvaluationParams params) {
        AtomicInteger n = new AtomicInteger((int) Math.round(this.a.evaluate(params)));
        int stop = (int) Math.round(this.b.evaluate(params));

        if (stop < n.get()) {
            throw new EvaluationException("Summation's stop must be >= its start. (" + stop + "<" + n + ")");
        }
        if (params.summationLoopLimit() > 0 && stop - n.get() > params.summationLoopLimit()) {
            throw new EvaluationException("Summation exceeds loop limit: " + params.summationLoopLimit());
        }

        Map<String, Supplier<Double>> params2 = new NHashMap(n);
        if (params.variables() != null) params2.putAll(params.variables());

        double sum = 0;
        do {
            params.timeoutManager().checkIn();
            sum += c.evaluate(new EvaluationParams(params2, new HashMap<>(), params.timeoutManager(), params.recursiveVariableLimit(), params.summationLoopLimit()));
        } while (n.incrementAndGet() <= stop);
        return sum;
    }

    private static class NHashMap extends HashMap<String, Supplier<Double>> {
        private final AtomicInteger n;

        NHashMap(AtomicInteger n) {
            this.n = n;
        }

        @Override
        public Supplier<Double> get(Object key) {
            if (key.equals("n")) return () -> (double) n.get();
            return super.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            if (key.equals("n")) return true;
            return super.containsKey(key);
        }
    }
}
