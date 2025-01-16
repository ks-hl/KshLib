package dev.kshl.kshlib.llm.embed;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

public class FloatEmbeddings extends AbstractEmbeddings {

    private final List<Float> embeddings;

    public FloatEmbeddings(List<Float> embeddings) {
        this.embeddings = embeddings.stream().toList();
    }

    @Override
    protected AbstractEmbeddings construct(List<Float> embeddings) {
        return new FloatEmbeddings(embeddings);
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size() * 4);
        for (Float embedding : this) {
            buffer.putFloat(embedding);
        }
        return buffer.array();
    }

    @Override
    public int size() {
        return this.embeddings.size();
    }

    @Override
    public Float get(int index) {
        return this.embeddings.get(index);
    }

    @Override
    public boolean isEmpty() {
        return this.embeddings.isEmpty();
    }

    @Override
    @Nonnull
    public Iterator<Float> iterator() {
        return this.embeddings.iterator();
    }
}
