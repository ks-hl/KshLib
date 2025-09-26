package dev.kshl.kshlib.net.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPUtil {


    public static boolean isIPv4(String ip) {
        // https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp
        return ip.matches("((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}");
    }

    public static boolean isIPv6(String ip) {
        // https://stackoverflow.com/questions/53497/regular-expression-that-matches-valid-ipv6-addresses
        return ip.matches("(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))");
    }

    public static int encodeV4(String ip) {
        return (int) encodeV4Long(ip);
    }

    public static long encodeV4Long(String ip) {
        if (!isIPv4(ip)) throw new IllegalArgumentException("Invalid IP: " + ip);

        String[] parts = ip.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Illegal IP, not 4 parts");
        long out = 0;
        for (int i = 0; i < 4; i++) {
            try {
                out |= (Long.parseLong(parts[i]) & 0xFF) << ((3 - i) * 8);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numerical IP address. " + ip + " (" + parts[i] + ")");
            }
        }
        return out;
    }

    public static byte[] toBytes(String ip) {
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }

    public static int encodeV4(byte[] bytes) {
        int out = 0;
        for (int i = 0; i < 4; i++) {
            out |= (((int) bytes[i]) & 0xFF) << ((3 - i) * 8);
        }
        return out;
    }

    public static String decodeV4(int ip) {
        return decodeV4((long) ip);
    }

    public static String decodeV4(long ip) {
        StringBuilder out = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            if (!out.isEmpty()) out.append(".");
            out.append((ip >> (8 * i)) & 0xFF);
        }
        return out.toString();
    }

    public static long getSubnetMask(int cidr) {
        if (cidr < 0) {
            throw new IllegalArgumentException("CIDR must be >= 0");
        }
        if (cidr > 32) {
            throw new IllegalArgumentException("CIDR must be <= 32");
        }
        long out = 0;
        for (int i = 0; i < cidr; i++) {
            if (i > 0) out <<= 1;
            out++;
        }
        if (cidr < 32) {
            out <<= 32 - cidr;
        }
        return out;
    }

    public static IPv4 getRangeStart(String ip, int cidr) {
        long ipLong = encodeV4Long(ip);
        ipLong &= getSubnetMask(cidr);
        return new IPv4(ipLong);
    }

    public static IPv4 getRangeEnd(String ip, int cidr) {
        long ipLong = encodeV4Long(ip);
        long mask = getSubnetMask(cidr);
        long start = ipLong & mask;
        long end = start | ~mask;
        return new IPv4(end);
    }

    private static String toNetworkPrefixV4(String ip) {
        long net = encodeV4Long(ip) & getSubnetMask(24);
        return decodeV4(net) + "/24";
    }

    public static String toNetworkPrefix(String ip) {
        if (isIPv4(ip)) return toNetworkPrefixV4(ip);

        if (isIPv6(ip)) {
            String literal = stripZone(ip); // avoid UnknownHostException for %zone
            byte[] bytes = toBytes(literal);
            if (bytes.length == 4) {
                return toNetworkPrefixV4(new IPv4(bytes).toString());
            }
            if (bytes.length != 16) {
                throw new IllegalArgumentException("Expected 16 bytes, was " + bytes.length);
            }
            // zero last 64 bits
            for (int i = 8; i < 16; i++) bytes[i] = 0;
            return formatIpv6(bytes) + "/64";
        }

        throw new IllegalArgumentException("Invalid IP: " + ip);
    }

    private static String stripZone(String ip) {
        int i = ip.indexOf('%');
        return (i >= 0) ? ip.substring(0, i) : ip;
    }

    // Deterministic, RFC 5952–style (closest reasonable) IPv6 compression.
    private static String formatIpv6(byte[] bytes) {
        // split into 8 hextets
        int[] h = new int[8];
        for (int i = 0; i < 8; i++) {
            h[i] = ((bytes[2 * i] & 0xFF) << 8) | (bytes[2 * i + 1] & 0xFF);
        }

        // find longest run of zeros (len >= 2) to compress
        int bestStart = -1, bestLen = 0;
        for (int i = 0; i < 8; ) {
            if (h[i] != 0) {
                i++;
                continue;
            }
            int j = i;
            while (j < 8 && h[j] == 0) j++;
            int len = j - i;
            if (len > bestLen && len >= 2) {
                bestStart = i;
                bestLen = len;
            }
            i = j;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; ) {
            if (i == bestStart) {
                // compress this zero run
                if (i == 0) sb.append("::");
                else sb.append(':').append(':');
                i += bestLen;
                if (i >= 8) break;
            } else {
                if (i > 0 && i != bestStart + bestLen) sb.append(':');
                sb.append(Integer.toHexString(h[i]));
                i++;
            }
        }

        String out = sb.toString();
        return out.isEmpty() ? "::" : out;
    }

}
