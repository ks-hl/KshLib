package dev.kshl.kshlib.llm.embed;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

@Getter
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

    public static FloatEmbeddings fromBytes(byte[] bytes) {
        List<Float> embeds = new ArrayList<>();
        for (int i = 0; i < bytes.length; i += 4) {
            int floatInt =
                    ((bytes[i] & 0xFF) << 24) |
                            ((bytes[i + 1] & 0xFF) << 16) |
                            ((bytes[i + 2] & 0xFF) << 8) |
                            (bytes[i + 3] & 0xFF);
            embeds.add(Float.intBitsToFloat(floatInt));
        }
        return new FloatEmbeddings(embeds);
    }

    public static FloatEmbeddings fromBase64(String base64) {
        return fromBytes(Base64.getDecoder().decode(base64));
    }
}
