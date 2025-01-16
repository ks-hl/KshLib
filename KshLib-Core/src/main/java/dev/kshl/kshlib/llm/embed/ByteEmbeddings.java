package dev.kshl.kshlib.llm.embed;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ByteEmbeddings extends AbstractEmbeddings {

    private final byte[] embeddings;

    public ByteEmbeddings(List<Float> embeddings) {
        this.embeddings = new byte[embeddings.size()];
        for (int i = 0; i < this.embeddings.length; i++) {
            this.embeddings[i] = (byte) (embeddings.get(i) * Byte.MAX_VALUE);
        }
    }

    @Override
    protected AbstractEmbeddings construct(List<Float> embeddings) {
        return new ByteEmbeddings(embeddings);
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(embeddings, embeddings.length);
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
        return new ByteIterator();
    }

    private static float toFloat(byte b) {
        return ((float) b) / Byte.MAX_VALUE;
    }

    private class ByteIterator implements Iterator<Float> {
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
