package dev.kshl.kshlib.net.ip;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NetworkPrefixTest {
    @Nested
    @DisplayName("IPv4 → /24")
    class Ipv4Tests {

        @ParameterizedTest(name = "[{index}] {0} → {1}")
        @CsvSource({
                "192.0.2.123, 192.0.2.0/24",
                "10.20.30.40, 10.20.30.0/24",
                "0.0.0.0, 0.0.0.0/24",
                "255.255.255.255, 255.255.255.0/24",
                "203.0.113.1, 203.0.113.0/24"
        })
        void ipv4To24(String ip, String expected) {
            assertEquals(expected, IPUtil.toNetworkPrefix(ip));
        }
    }

    @Nested
    @DisplayName("IPv6 → /64")
    class Ipv6Tests {

        @ParameterizedTest(name = "[{index}] {0} → {1}")
        @CsvSource({
                // canonical compression should be preserved by Inet6Address
                "2001:db8::1, 2001:db8::/64",
                "2001:db8:0:1:2:3:4:5, 2001:db8:0:1::/64",
                "::1, ::/64",
                // link-local with scope/zone id: scope must be dropped in output
                "fe80::1%eth0, fe80::/64",
        })
        void ipv6To64(String ip, String expected) {
            assertEquals(expected, IPUtil.toNetworkPrefix(ip));
        }

        @ParameterizedTest(name = "[{index}] mapped {0} → {1}")
        @CsvSource({
                // IPv6-mapped IPv4 addresses: zeroing the last 64 bits yields ::/64
                "::ffff:192.0.2.45, 192.0.2.0/24",
                "::ffff:0:203.0.113.10, ::/64"
        })
        void ipv6MappedTo64(String ip, String expected) {
            assertEquals(expected, IPUtil.toNetworkPrefix(ip));
        }
    }

    @Nested
    @DisplayName("Invalid inputs")
    class InvalidInputs {

        @ParameterizedTest
        @ValueSource(strings = {
                "", "   ", "example.com",
                "999.0.0.1", "1.2.3", "1.2.3.4.5",
                "gggg::1", "2001:db8:::1"
        })
        void rejectsInvalid(String ip) {
            assertThrows(IllegalArgumentException.class, () -> IPUtil.toNetworkPrefix(ip));
        }
    }
}
