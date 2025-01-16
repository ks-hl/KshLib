package dev.kshl.kshlib.llm.embed;

import dev.kshl.kshlib.json.JSONCollector;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public abstract class AbstractEmbeddings {
    private final List<Float> embeddings;

    public AbstractEmbeddings(List<Float> embeddings) {
        this.embeddings = embeddings.stream().toList();
    }

    public AbstractEmbeddings(JSONArray jsonArray) {
        this(fromJSON(jsonArray));
    }

    public AbstractEmbeddings(byte[] bytes) {
        List<Float> embeds = new ArrayList<>();
        for (int i = 0; i < bytes.length; i += 4) {
            int floatInt =
                    ((bytes[i] & 0xFF) << 24) |
                            ((bytes[i + 1] & 0xFF) << 16) |
                            ((bytes[i + 2] & 0xFF) << 8) |
                            (bytes[i + 3] & 0xFF);
            embeds.add(Float.intBitsToFloat(floatInt));
        }
        this.embeddings = Collections.unmodifiableList(embeds);
    }

    public static AbstractEmbeddings fromIntList(List<Integer> intList) {
        List<Float> embeddings = new ArrayList<>();
        for (Integer i : intList) {
            embeddings.add((float) i / Integer.MAX_VALUE);
        }
        return new AbstractEmbeddings(embeddings);
    }

    public static AbstractEmbeddings fromNestedList(List<List<Float>> embeddings) {
        List<Float> combined = new ArrayList<>();
        for (List<Float> embedding : embeddings) {
            combined.addAll(embedding);
        }
        return new AbstractEmbeddings(combined);
    }

    private static List<Float> fromJSON(JSONArray array) {
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

    public List<Integer> getEmbeddingsIntList() {
        return getEmbeddings().stream().map(f -> (int) (f * Integer.MAX_VALUE)).toList();
    }

    public JSONArray toJSON() {
        return this.getEmbeddings().stream().collect(new JSONCollector());
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(getEmbeddings().size() * 4);
        for (Float embedding : getEmbeddings()) {
            buffer.putFloat(embedding);
        }
        return buffer.array();
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(getBytes());
    }

    public static AbstractEmbeddings fromBase64(String base64) {
        return construct(Base64.getDecoder().decode(base64));
    }

    @Override
    public int hashCode() {
        return getEmbeddings().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbstractEmbeddings otherEmbeddings)) return false;
        return this.getEmbeddings().equals(otherEmbeddings.getEmbeddings());
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public abstract List<Float> getEmbeddings();

    /**
     * Calculates the cosine similarity between two embeddings.
     *
     * @param other The other Embeddings instance to compare with.
     * @return The cosine similarity between the two embeddings.
     * @throws IllegalArgumentException if the embeddings have different dimensions or are empty.
     */
    public double compareCosine(AbstractEmbeddings other) {
        if (this.getEmbeddings().size() != other.getEmbeddings().size()) {
            throw new IllegalArgumentException("Embeddings must have the same number of elements. " + this.getEmbeddings().size() + "!=" + other.getEmbeddings().size());
        }
        if (this.getEmbeddings().isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be empty");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < this.getEmbeddings().size(); i++) {
            double a = this.getEmbeddings().get(i);
            double b = other.getEmbeddings().get(i);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("Embeddings cannot have zero norm");
        }

        return dotProduct / Math.sqrt(normA * normB);
    }

    /**
     * Calculates the cosine similarity between two embeddings.
     *
     * @param other The other Embeddings instance to compare with.
     * @return The cosine similarity between the two embeddings.
     * @throws IllegalArgumentException if the embeddings have different dimensions or are empty.
     */
    public double compareEuclidean(AbstractEmbeddings other) {
        if (this.getEmbeddings().size() != other.getEmbeddings().size()) {
            throw new IllegalArgumentException("Embeddings must have the same number of elements. " + this.getEmbeddings().size() + "!=" + other.getEmbeddings().size());
        }
        if (this.getEmbeddings().isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be empty");
        }

        double sum = 0.0;

        for (int i = 0; i < this.getEmbeddings().size(); i++) {
            double a = this.getEmbeddings().get(i);
            double b = other.getEmbeddings().get(i);

            sum += (a - b) * (a - b);
        }

        return Math.sqrt(sum);
    }

    /**
     * Truncates the vector after numComponents elements
     */
    public AbstractEmbeddings getFirstN(int numComponents) {
        List<Float> embeddings = new ArrayList<>();
        for (Float embedding : getEmbeddings()) {
            embeddings.add(embedding);
            if (embeddings.size() >= numComponents) break;
        }
        return construct(embeddings);
    }

    protected abstract AbstractEmbeddings construct(List<Float> embeddings);
}