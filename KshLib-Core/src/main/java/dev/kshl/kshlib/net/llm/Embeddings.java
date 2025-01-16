package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.json.JSONCollector;
import dev.kshl.kshlib.json.JSONUtil;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Embeddings {
    private final List<Float> embeddings;

    public Embeddings(List<List<Float>> embeddings) {
        List<Float> combined = new ArrayList<>();
        for (List<Float> embedding : embeddings) {
            combined.addAll(embedding);
        }
        this.embeddings = Collections.unmodifiableList(combined);
    }

    public static Embeddings fromJSON(JSONArray json) {
        return new Embeddings(JSONUtil.stream(json)
                .map(o -> (JSONArray) o)
                .map(o -> JSONUtil.stream(o)
                        .map(o1 -> ((BigDecimal) o1).floatValue()).toList()
                ).toList());
    }

    public JSONArray toJSON() {
        return this.embeddings.stream().collect(new JSONCollector());
    }

    public void put(PreparedStatement preparedStatement, int index) {
//        preparedStatement.set
    }

    @Override
    public int hashCode() {
        return embeddings.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Embeddings otherEmbeddings)) return false;
        return this.embeddings.equals(otherEmbeddings.embeddings);
    }

    /**
     * Compares two embeddings and returns their cosine similarity.
     *
     * @return The cosine similarity between the two embeddings.
     * @throws IllegalArgumentException if the embeddings have different dimensions or are empty.
     */
    public double compareTo(Embeddings other) {
        if (this.embeddings.size() != other.embeddings.size()) {
            throw new IllegalArgumentException("Embeddings must have the same number of vectors");
        }
        if (this.embeddings.isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be empty");
        }

        double totalSimilarity = cosineSimilarity(this.embeddings, other.embeddings);

        return totalSimilarity / this.embeddings.size();
    }

    private static double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must be of the same length");
        }
        if (vectorA.isEmpty()) {
            throw new IllegalArgumentException("Vectors cannot be empty");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            double a = vectorA.get(i);
            double b = vectorB.get(i);

            dotProduct += a * b;
            normA += Math.pow(a, 2);
            normB += Math.pow(b, 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("Vectors cannot have zero norm");
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
