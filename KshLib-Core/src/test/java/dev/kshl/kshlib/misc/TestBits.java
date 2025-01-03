package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBits {
    private static final byte ALL_ONES = (byte) 0b11111111;

    @Test
    public void testBits() {
        byte val = 0b0;
        byte target = (byte) 0b01100010;
        for (int i = 0; i < 8; i++) {
            val = Bits.setBit(val, i, Bits.getBit(target, i));
        }
        assertEquals(target, val);
    }

    @Test
    public void testToFromByteArrayLong() {
        assertEquals(1L, Bits.toLong(Bits.toBytes(1L)));
        assertEquals(0L, Bits.toLong(Bits.toBytes(0L)));
        assertEquals(-1L, Bits.toLong(Bits.toBytes(-1L)));
        assertEquals(Long.MAX_VALUE, Bits.toLong(Bits.toBytes(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, Bits.toLong(Bits.toBytes(Long.MIN_VALUE)));
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, Bits.toBytes(0L));
        assertArrayEquals(new byte[]{0b01111111, ALL_ONES, ALL_ONES, ALL_ONES, ALL_ONES, ALL_ONES, ALL_ONES, ALL_ONES}, Bits.toBytes(Long.MAX_VALUE));
        assertArrayEquals(new byte[]{(byte) 0b10000000, 0, 0, 0, 0, 0, 0, 0}, Bits.toBytes(Long.MIN_VALUE));
    }

    @Test
    public void testToFromByteArrayInt() {
        assertEquals(1, Bits.toInt(Bits.toBytes(1)));
        assertEquals(0, Bits.toInt(Bits.toBytes(0)));
        assertEquals(-1, Bits.toInt(Bits.toBytes(-1)));
        assertEquals(Integer.MAX_VALUE, Bits.toInt(Bits.toBytes(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, Bits.toInt(Bits.toBytes(Integer.MIN_VALUE)));
        assertArrayEquals(new byte[]{0, 0, 0, 0}, Bits.toBytes(0));
        assertArrayEquals(new byte[]{0b01111111, ALL_ONES, ALL_ONES, ALL_ONES}, Bits.toBytes(Integer.MAX_VALUE));
        assertArrayEquals(new byte[]{(byte) 0b10000000, 0, 0, 0}, Bits.toBytes(Integer.MIN_VALUE));
    }

    @Test
    public void testToFromByteArrayShort() {
        assertEquals(1, Bits.toShort(Bits.toBytes((short) 1)));
        assertEquals(0, Bits.toShort(Bits.toBytes((short) 0)));
        assertEquals(-1, Bits.toShort(Bits.toBytes((short) -1)));
        assertEquals(Short.MAX_VALUE, Bits.toShort(Bits.toBytes(Short.MAX_VALUE)));
        assertEquals(Short.MIN_VALUE, Bits.toShort(Bits.toBytes(Short.MIN_VALUE)));
        assertArrayEquals(new byte[]{0, 0}, Bits.toBytes((short) 0));
        assertArrayEquals(new byte[]{0b01111111, ALL_ONES}, Bits.toBytes(Short.MAX_VALUE));
        assertArrayEquals(new byte[]{(byte) 0b10000000, 0}, Bits.toBytes(Short.MIN_VALUE));
    }

    @Test
    public void testToFromByteArrayByte() {
        assertEquals(1, Bits.toByte(Bits.toBytes((byte) 1)));
        assertEquals(0, Bits.toByte(Bits.toBytes((byte) 0)));
        assertEquals(-1, Bits.toByte(Bits.toBytes((byte) -1)));
        assertEquals(Byte.MAX_VALUE, Bits.toByte(Bits.toBytes(Byte.MAX_VALUE)));
        assertEquals(Byte.MIN_VALUE, Bits.toByte(Bits.toBytes(Byte.MIN_VALUE)));
        assertArrayEquals(new byte[]{0}, Bits.toBytes((byte) 0));
        assertArrayEquals(new byte[]{0b01111111}, Bits.toBytes(Byte.MAX_VALUE));
        assertArrayEquals(new byte[]{(byte) 0b10000000}, Bits.toBytes(Byte.MIN_VALUE));
    }

    @Test
    public void testByteToBinaryString() {
        assertEquals("00000000", Bits.toBinaryString((byte) 0b00000000));
        assertEquals("00000010", Bits.toBinaryString((byte) 0b00000010));
        assertEquals("00000100", Bits.toBinaryString((byte) 0b00000100));
        assertEquals("00001000", Bits.toBinaryString((byte) 0b00001000));
        assertEquals("00010000", Bits.toBinaryString((byte) 0b00010000));
        assertEquals("00100000", Bits.toBinaryString((byte) 0b00100000));
        assertEquals("01000000", Bits.toBinaryString((byte) 0b01000000));
        assertEquals("11111111", Bits.toBinaryString((byte) 0b11111111));
    }

    @Test
    public void testPad() {
        assertArrayEquals(new byte[]{Byte.MAX_VALUE}, Bits.pad(new byte[]{Byte.MAX_VALUE}, 0));
        assertArrayEquals(new byte[]{Byte.MAX_VALUE, 0}, Bits.pad(new byte[]{Byte.MAX_VALUE}, 1));
        assertArrayEquals(new byte[]{Byte.MIN_VALUE, 0, 0}, Bits.pad(new byte[]{Byte.MIN_VALUE}, 2));
    }
}
