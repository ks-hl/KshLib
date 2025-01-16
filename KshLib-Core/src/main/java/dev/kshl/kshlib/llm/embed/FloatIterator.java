package dev.kshl.kshlib.llm.embed;

import java.util.Iterator;

class FloatIterator implements Iterator<Float> {
    protected final float[] array;
    private int index;

    public FloatIterator(float[] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public Float next() {
        return get(index++);
    }

    protected float get(int index) {
        return array[index];
    }
}
