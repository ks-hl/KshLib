package dev.kshl.kshlib.llm.embed;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

public class ShortEmbeddings extends AbstractEmbeddings {

    private final short[] embeddings;

    public ShortEmbeddings(List<Float> embeddings) {
        this.embeddings = new short[embeddings.size()];
        for (int i = 0; i < this.embeddings.length; i++) {
            this.embeddings[i] = (short) (embeddings.get(i) * Short.MAX_VALUE);
        }
    }

    @Override
    protected AbstractEmbeddings construct(List<Float> embeddings) {
        return new ShortEmbeddings(embeddings);
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size() * 2);
        for (Short embedding : this.embeddings) {
            buffer.putShort(embedding);
        }
        return buffer.array();
    }

    @Override
    public int size() {
        return this.embeddings.length;
    }

    @Override
    public Float get(int index) {
        return toFloat(this.embeddings[index]);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    @Nonnull
    public Iterator<Float> iterator() {
        return new ShortIterator();
    }

    private static float toFloat(short s) {
        return ((float) s) / Short.MAX_VALUE;
    }

    private class ShortIterator implements Iterator<Float> {
        private int index;

        @Override
        public boolean hasNext() {
            return index < embeddings.length;
        }

        @Override
        public Float next() {
            return toFloat(embeddings[index++]);
        }
    }
}
