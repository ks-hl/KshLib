package dev.kshl.kshlib.llm.embed;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

public class FloatEmbeddings extends AbstractEmbeddings {

    private final float[] embeddings;

    public FloatEmbeddings(List<Float> embeddings) {
        this.embeddings = new float[embeddings.size()];
        for (int i = 0; i < this.embeddings.length; i++) {
            this.embeddings[i] = (embeddings.get(i));
        }
    }

    @Override
    protected AbstractEmbeddings construct(List<Float> embeddings) {
        return new ShortEmbeddings(embeddings);
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size() * 4);
        for (Float embedding : this.embeddings) {
            buffer.putFloat(embedding);
        }
        return buffer.array();
    }

    @Override
    public int size() {
        return this.embeddings.length;
    }

    @Override
    public Float get(int index) {
        return this.embeddings[index];
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    @Nonnull
    public Iterator<Float> iterator() {
        return new FloatIterator(this.embeddings);
    }

}
