package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBitBuffer {
    private BitBuffer bitBuffer;

    @BeforeEach
    public void setUp() {
        bitBuffer = new BitBuffer(4); // 4 bytes = 32 bits
    }

    @Test
    public void testPutAndGet_SimpleCase() {
        byte[] input = {(byte) 0b10101010, (byte) 0b01010101};
        bitBuffer.put(0, input, 0, 16);
        byte[] result = bitBuffer.get(0, 16);
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));
        assertArrayEquals(input, result, "The retrieved bits should match the input bits");
    }

    @Test
    public void testPutAndGet_OffsetInBuffer() {
        byte[] input = {(byte) 0b10101010, (byte) 0b01010101};
        bitBuffer.put(8, input, 0, 16); // Insert starting from the 8th bit (2nd byte)
        byte[] result = bitBuffer.get(8, 24);
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));
        assertArrayEquals(input, result, "The retrieved bits should match the input bits when offset in the buffer");
    }

    @Test
    public void testPutAndGet_PartialByte() {
        byte[] input = {(byte) 0b10101010}; // 8 bits
        bitBuffer.put(0, input, 2, 6); // Insert bits 2 to 6 (0-indexed, so bits 3-6)
        byte[] expected = {(byte) 0b10100000}; // Expected to extract 4 bits: 0101 -> padded to 00101000
        byte[] result = bitBuffer.get(0, 4);
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));
        assertEquals(Bits.toBinaryString(expected), Bits.toBinaryString(result), "Partial byte insertion should result in correct bit retrieval");
    }

    @Test
    public void testPutAndGet_OverlappingBits() {
        byte[] input1 = {(byte) 0b11110000};
        byte[] input2 = {(byte) 0b00001111};

        bitBuffer.put(0, input1, 0, 8);
        bitBuffer.put(4, input2, 0, 8); // Overlap, starting at the 5th bit
        byte[] result = bitBuffer.get(0, 12); // First byte and the first half of the second byte

        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));

        byte[] expected = {(byte) 0b11110000, (byte) 0b11110000}; // 1111 from input1 and 1111 from input2
        assertEquals(Bits.toBinaryString(expected), Bits.toBinaryString(result), "Overlapping bit insertions should be correctly retrieved");
    }

    @Test
    public void testPutAndGet_BeyondBufferLimits() {
        byte[] input = {(byte) 0b11110000};

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            bitBuffer.put(28, input, 0, 8); // This would overflow the 32-bit buffer
        });

        assertTrue(exception.getMessage().contains("Tried to insert"));
    }

    @Test
    public void testGet_PartialBits() {
        byte[] input = {(byte) 0b11110000};
        bitBuffer.put(0, input, 0, 8);
        byte[] result = bitBuffer.get(2, 6); // Extract middle bits

        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));

        byte[] expected = {(byte) 0b11000000}; // Expected: 1111 -> padded to 00111100
        assertEquals(Bits.toBinaryString(expected), Bits.toBinaryString(result), "Partial bit extraction should retrieve correct bits");
    }

    @Test
    public void testPutAndGet_MultipleByteArrays() {
        byte[] input1 = {(byte) 0b11110000};
        byte[] input2 = {(byte) 0b00001111};

        bitBuffer.put(0, input1, 0, 8);
        bitBuffer.put(8, input2, 0, 8);
        byte[] result = bitBuffer.get(0, 16);

        byte[] expected = {(byte) 0b11110000, (byte) 0b00001111};
        assertEquals(Bits.toBinaryString(expected), Bits.toBinaryString(result), "Multiple byte arrays should be correctly placed in buffer");
    }

    @Test
    public void testPut_InvalidStartStopIndices() {
        byte[] input = {(byte) 0b11110000};

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.put(0, input, 10, 5),
                "start > stop should throw IndexOutOfBoundsException");

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.put(0, input, -1, 5),
                "Negative start index should throw IndexOutOfBoundsException");

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.put(0, input, 0, 10),
                "stop index beyond array length should throw IndexOutOfBoundsException");
    }

    @Test
    public void testGet_InvalidStartStopIndices() {
        byte[] input = {(byte) 0b11110000};
        bitBuffer.put(0, input, 0, 8);

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.get(-1, 5),
                "Negative start index should throw IndexOutOfBoundsException");

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.get(0, 40),
                "stop index beyond buffer length should throw IndexOutOfBoundsException");

        assertThrows(IndexOutOfBoundsException.class, () -> bitBuffer.get(10, 5),
                "start > stop should throw IndexOutOfBoundsException");
    }

    @Test
    public void testPutAndGet_MultipleOperations() {
        byte[] input1 = {(byte) 0b11000000};
        byte[] input2 = {(byte) 0b00111100};
        byte[] input3 = {(byte) 0b00000011};

        bitBuffer.put(0, input1, 0, 8);
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));
        bitBuffer.put(4, input2, 2, 6); // Overlapping middle bits
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));
        bitBuffer.put(12, input3, 6, 8);
        System.out.println("Buffer: " + Bits.toBinaryString(bitBuffer.array()));

        byte[] result = bitBuffer.get(0, 16);
        byte[] expected = {(byte) 0b11001111, (byte) 0b00001100};
        assertEquals(Bits.toBinaryString(expected), Bits.toBinaryString(result), "Multiple put operations should result in correctly composed buffer");
    }
}
