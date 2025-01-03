package dev.kshl.kshlib.misc;

public class BitBuffer {
    private final byte[] array;

    public BitBuffer(int numberOfBytes) {
        this(new byte[numberOfBytes]);
    }

    public BitBuffer(byte[] bytes) {
        this.array = bytes;
    }

    public byte[] array() {
        return array;
    }

    /**
     * Inserts the provided byte[] into this buffer
     *
     * @param index The bit index in the buffer array to start with
     * @param arr   The array to add
     * @param start The bit index in the provided array to start with
     * @param stop  The bit index in the provided array to stop with
     * @return this
     */
    public BitBuffer put(int index, byte[] arr, int start, int stop) {
        if (start < 0 || stop > arr.length * 8 || start > stop) {
            throw new IndexOutOfBoundsException("Invalid bit range");
        }
        if (index + stop - start > array.length * 8) {
            throw new IndexOutOfBoundsException("Tried to insert " + (stop - start) + " bits with only " + (array.length * 8) + " bits of space");
        }

        int bitIndex = start;
        for (int i = index; i < index + (stop - start); i++) {
            int byteIndex = i / 8;
            int bitOffset = 7 - (i % 8);

            int sourceByteIndex = bitIndex / 8;
            int sourceBitOffset = 7 - (bitIndex % 8);

            byte bit = (byte) ((arr[sourceByteIndex] >>> sourceBitOffset) & 1);
            array[byteIndex] &= (byte) ~(1 << bitOffset);
            array[byteIndex] |= (byte) (bit << bitOffset);

            bitIndex++;
        }

        return this;
    }

    /**
     * Gits a subsection of this buffer. Any bits not divisible by 8 will be padded with zeroes in the last byte
     *
     * @param start The bit index in the buffer array to start with
     * @param stop  The bit index in the buffer array to stop with
     * @return The subsection byte array
     */
    public byte[] get(int start, int stop) {
        if (start < 0 || stop > array.length * 8 || start > stop) {
            throw new IndexOutOfBoundsException("Invalid bit range");
        }

        byte[] out = new byte[(stop - start - 1) / 8 + 1];

        int bitIndex = start;
        for (int i = 0; i < (stop - start); i++) {
            int byteIndex = i / 8;
            int bitOffset = 7 - (i % 8);

            int sourceByteIndex = bitIndex / 8;
            int sourceBitOffset = 7 - (bitIndex % 8);

            byte bit = (byte) ((array[sourceByteIndex] >>> sourceBitOffset) & 1);
            out[byteIndex] &= (byte) ~(1 << bitOffset);
            out[byteIndex] |= (byte) (bit << bitOffset);

            bitIndex++;
        }

        return out;
    }

    public int size() {
        return array.length * 8;
    }
}
