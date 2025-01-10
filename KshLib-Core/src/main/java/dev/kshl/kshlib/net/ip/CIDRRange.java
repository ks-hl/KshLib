package dev.kshl.kshlib.net.ip;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CIDRRange implements Iterable<IPv4> {
    private final IPv4 rangeStart;
    private final IPv4 rangeEnd;
    private final int cidr;

    public CIDRRange(String ipv4, int cidr) {
        this.cidr = cidr;
        if (!IPUtil.isIPv4(ipv4)) {
            throw new IllegalArgumentException("Invalid IP: " + ipv4);
        }
        this.rangeStart = IPUtil.getRangeStart(ipv4, cidr);
        this.rangeEnd = IPUtil.getRangeEnd(ipv4, cidr);
    }

    public CIDRRange(String ipv4WithCIDR) {
        this(ipv4WithCIDR.split("/")[0], getCIDR(ipv4WithCIDR));
    }

    private static int getCIDR(String ipv4WithCIDR) {
        String[] split = ipv4WithCIDR.split("/");
        if (split.length != 2) throw new IllegalArgumentException("Invalid formatted CIDR range");
        return Integer.parseInt(split[1]);
    }

    @Override
    @Nonnull
    public Iterator<IPv4> iterator() {
        return new CIDRIterator(rangeStart, rangeEnd);
    }

    public long size() {
        long out = 1;
        for (int i = 0; i < 32 - cidr; i++) {
            out *= 2L;
        }
        return out;
    }

    public static final class CIDRIterator implements Iterator<IPv4> {
        private IPv4 current;
        private final IPv4 start;
        private final IPv4 end;

        private CIDRIterator(IPv4 start, IPv4 end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            if (current == null) return true;
            return current.compareTo(end) < 0;
        }

        @Override
        public IPv4 next() {
            if (!hasNext()) throw new NoSuchElementException();
            if (current == null) return current = start;
            return current = current.increment();
        }
    }

    @Override
    public String toString() {
        return rangeStart + "/" + cidr;
    }
}
