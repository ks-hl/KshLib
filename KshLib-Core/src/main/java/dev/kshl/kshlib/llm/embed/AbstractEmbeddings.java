package dev.kshl.kshlib.llm.embed;

import dev.kshl.kshlib.json.JSONCollector;
import org.json.JSONArray;

import java.util.AbstractList;
import java.util.Base64;
import java.util.List;

public abstract class AbstractEmbeddings extends AbstractList<Float> {

    public List<Integer> getEmbeddingsIntList() {
        return stream().map(f -> (int) (f * Integer.MAX_VALUE)).toList();
    }

    public JSONArray toJSON() {
        return stream().collect(new JSONCollector());
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(getBytes());
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    /**
     * Calculates the cosine similarity between two embeddings.
     *
     * @param other The other Embeddings instance to compare with.
     * @return The cosine similarity between the two embeddings.
     * @throws IllegalArgumentException if the embeddings have different dimensions or are empty.
     */
    public double compareCosine(AbstractEmbeddings other) {
        if (this.size() != other.size()) {
            throw new IllegalArgumentException("Embeddings must have the same number of elements. " + this.size() + "!=" + other.size());
        }
        if (this.isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be empty");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < this.size(); i++) {
            double a = this.get(i);
            double b = other.get(i);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("Embeddings cannot have zero norm");
        }

        return dotProduct / Math.sqrt(normA * normB);
    }

    protected abstract AbstractEmbeddings construct(List<Float> embeddings);

    public abstract byte[] getBytes();
}