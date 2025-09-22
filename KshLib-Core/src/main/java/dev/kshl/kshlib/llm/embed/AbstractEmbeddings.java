package dev.kshl.kshlib.llm.embed;

import dev.kshl.kshlib.json.JSONCollector;
import org.json.JSONArray;

import java.util.AbstractList;

public abstract class AbstractEmbeddings extends AbstractList<Float> {
    public JSONArray toJSON() {
        return stream().collect(new JSONCollector());
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}