package dev.kshl.kshlib.net.ip;

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
        if (!isIPv4(ip)) throw new IllegalArgumentException("Invalid IP: " + ip);

        String[] parts = ip.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Illegal IP, not 4 parts");
        byte[] out = new byte[4];
        for (int i = 0; i < 4; i++) {
            try {
                out[i] = (byte) (Integer.parseInt(parts[i]) & 0xFF);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numerical IP address. " + ip + " (" + parts[i] + ")");
            }
        }
        return out;
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
}
