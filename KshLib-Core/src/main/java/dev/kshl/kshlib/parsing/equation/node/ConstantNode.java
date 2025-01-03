package dev.kshl.kshlib.parsing.equation.node;

import java.util.function.Supplier;

/**
 * A static value with a name
 */
public class ConstantNode extends NumberNode {
    private final Supplier<String> name;

    public ConstantNode(double value, Supplier<String> name, ConstructorParams params) {
        super(value, params);
        this.name = name;
    }

    @Override
    public String toString(boolean reduce) {
        if (reduce) return super.toString(false);

        return name.get();
    }
}
