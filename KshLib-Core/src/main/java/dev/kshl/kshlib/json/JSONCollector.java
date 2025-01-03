package dev.kshl.kshlib.json;

import org.json.JSONArray;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class JSONCollector implements Collector<Object, JSONArray, JSONArray> {

    @Override
    public Supplier<JSONArray> supplier() {
        return JSONArray::new;
    }

    @Override
    public BiConsumer<JSONArray, Object> accumulator() {
        return JSONArray::put;
    }

    @Override
    public BinaryOperator<JSONArray> combiner() {
        return (left, right) -> {
            left.putAll(right);
            return left;
        };
    }

    @Override
    public Function<JSONArray, JSONArray> finisher() {
        return i -> i;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collectors.toList().characteristics();
    }

    public static JSONCollector toJSON() {
        return new JSONCollector();
    }
}
