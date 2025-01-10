package dev.kshl.kshlib.net.ip;

import javax.annotation.Nonnull;

public class IPv4 implements Comparable<IPv4> {
    private final byte a, b, c, d;

    public IPv4(byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Invalid byte array length, must be 4, got " + bytes.length);
        }
        this.a = bytes[0];
        this.b = bytes[1];
        this.c = bytes[2];
        this.d = bytes[3];
    }

    public IPv4(byte a, byte b, byte c, byte d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public IPv4(String ip) {
        this(IPUtil.toBytes(ip));
    }

    public IPv4(long ip) {
        this(IPUtil.decodeV4(ip));
    }

    public IPv4(int ip) {
        this(IPUtil.decodeV4(ip));
    }

    public IPv4 increment() {
        byte[] bytes = getBytes();
        if (++bytes[3] == 0) {
            if (++bytes[2] == 0) {
                if (++bytes[1] == 0) {
                    ++bytes[0];
                }
            }
        }
        return new IPv4(bytes);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < getBytes().length; i++) {
            if (i > 0) out.append(".");
            out.append(getByte(i));
        }
        return out.toString();
    }

    public byte[] getBytes() {
        return new byte[]{a, b, c, d};
    }

    public int toInt() {
        return IPUtil.encodeV4(getBytes());
    }

    @Override
    public int compareTo(@Nonnull IPv4 o) {
        long difference = difference(o);
        if (difference > 0) return 1;
        if (difference < 0) return -1;
        return 0;
    }

    public long difference(@Nonnull IPv4 o) {
        long difference = 0;
        for (int i = 0; i < 4; i++) {
            long me = getByte(i);
            long other = o.getByte(i);
            difference += (me - other) << ((3 - i) * 8);
        }
        return difference;
    }

    private int getByte(int i) {
        int val = switch (i) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            case 3 -> d;
            default -> throw new IndexOutOfBoundsException("Byte " + i + " does not exist. Only 0-3.");
        };
        if (val < 0) val += 256;
        return val;
    }
}
