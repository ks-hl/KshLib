package dev.kshl.kshlib.llm.embed;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Embeddings {

    public static AbstractEmbeddings fromIntList(List<Integer> intList) {
        List<Float> embeddings = new ArrayList<>();
        for (Integer i : intList) {
            embeddings.add((float) i / Integer.MAX_VALUE);
        }
        return new FloatEmbeddings(embeddings);
    }

    public static AbstractEmbeddings fromNestedList(List<List<Float>> embeddings) {
        List<Float> combined = new ArrayList<>();
        for (List<Float> embedding : embeddings) {
            combined.addAll(embedding);
        }
        return new FloatEmbeddings(combined);
    }

    public static AbstractEmbeddings fromJSON(JSONArray array) {
        return new FloatEmbeddings(fromJSONToList(array));
    }

    private static List<Float> fromJSONToList(JSONArray array) {
        List<Float> embeddings = new ArrayList<>();
        addAll(array, embeddings);
        return embeddings;
    }

    private static void addAll(JSONArray array, List<Float> embeddings) {
        for (Object o : array) {
            if (o instanceof JSONArray jsonArray) addAll(jsonArray, embeddings);
            else if (o instanceof BigDecimal bigDecimal) embeddings.add(bigDecimal.floatValue());
            else if (o instanceof Double d) embeddings.add(d.floatValue());
            else if (o instanceof Float f) embeddings.add(f);
            else if (o instanceof String s) embeddings.add(Float.parseFloat(s));
            else throw new IllegalArgumentException("Unexpected data type: " + o.getClass().getName());
        }
    }
}
