package dev.kshl.kshlib.misc;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

public class Bits {
    public static byte setBit(byte b, int index, boolean value) {
        if (index > 7 || index < 0) throw new IndexOutOfBoundsException(index + " is not a valid byte index.");
        byte val = (byte) (1 << index);
        if (value) b |= val;
        else b &= (byte) ~val;
        return b;
    }

    public static boolean getBit(byte b, int index) {
        if (index > 7 || index < 0) throw new IndexOutOfBoundsException(index + " is not a valid byte index.");
        return ((b >> index) & 1) == 1;
    }

    public static byte[] toBytes(byte value) {
        return new byte[]{value};
    }

    public static byte toByte(byte[] bytes) {
        return toByte(ByteBuffer.wrap(bytes));
    }

    public static byte toByte(ByteBuffer byteBuffer) {
        if (byteBuffer.array().length - byteBuffer.position() < 1) {
            throw new IllegalArgumentException("Invalid byte array length for a byte. len=" + byteBuffer.array().length);
        }
        return byteBuffer.get();
    }

    public static byte[] toBytes(short value) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort(value);
        return buffer.array();
    }

    public static short toShort(byte[] bytes) {
        return toShort(ByteBuffer.wrap(bytes));
    }

    public static short toShort(ByteBuffer byteBuffer) {
        if (byteBuffer.array().length - byteBuffer.position() < 2) {
            throw new IllegalArgumentException("Invalid byte array length for a short. len=" + byteBuffer.array().length);
        }
        return byteBuffer.getShort();
    }

    public static byte[] toBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(value);
        return buffer.array();
    }

    public static int toInt(byte[] bytes) {
        return toInt(ByteBuffer.wrap(bytes));
    }

    public static int toInt(ByteBuffer byteBuffer) {
        if (byteBuffer.array().length - byteBuffer.position() < 4) {
            throw new IllegalArgumentException("Invalid byte array length for an int. len=" + byteBuffer.array().length);
        }
        return byteBuffer.getInt();
    }

    public static byte[] toBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        return buffer.array();
    }

    public static long toLong(byte[] bytes) {
        return toLong(ByteBuffer.wrap(bytes));
    }

    public static long toLong(ByteBuffer byteBuffer) {
        if (byteBuffer.array().length - byteBuffer.position() < 8) {
            throw new IllegalArgumentException("Invalid byte array length for a long. len=" + byteBuffer.array().length);
        }
        return byteBuffer.getLong();
    }

    public static ByteBuffer truncateAfterPosition(ByteBuffer byteBuffer) {
        ByteBuffer out = ByteBuffer.allocate(byteBuffer.position());
        byteBuffer.rewind();
        byteBuffer.get(out.array());
        return out;
    }

    public static ByteBuffer slice(ByteBuffer byteBuffer, int from, int to) {
        return ByteBuffer.wrap(Arrays.copyOfRange(byteBuffer.array(), from, to));
    }

    public static ByteBuffer slice(ByteBuffer byteBuffer, int from) {
        return slice(byteBuffer, from, byteBuffer.array().length);
    }

    public static String encodeToHex(byte[] byteArray) {
        return HexFormat.of().formatHex(byteArray);
    }

    public static byte[] concat(byte[]... bytes) {
        int size = 0;
        for (byte[] aByte : bytes) {
            size += aByte.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (byte[] aByte : bytes) {
            buffer.put(aByte);
        }
        return buffer.array();
    }

    public static String toBinaryString(byte b) {
        StringBuilder builder = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            builder.append((b >>> i & 1) == 1 ? "1" : "0");
        }
        return builder.toString();
    }

    public static String toBinaryString(byte[] b) {
        StringBuilder builder = new StringBuilder();
        for (byte by : b) {
            if (!builder.isEmpty()) builder.append(" ");
            builder.append(toBinaryString(by));
        }
        return builder.toString();
    }

    public static byte[] pad(byte[] array, int add) {
        byte[] out = new byte[array.length + add];
        System.arraycopy(array, 0, out, 0, array.length);
        return out;
    }

    public static int leftShiftSigned(int value, int shift) {
        boolean negative = value < 0;
        value <<= shift;

        if (negative) value |= Integer.MIN_VALUE;
        else value &= Integer.MAX_VALUE;

        return value;
    }
}
