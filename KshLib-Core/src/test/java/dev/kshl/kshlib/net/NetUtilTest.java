package dev.kshl.kshlib.net;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateException;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NetUtilTest {
    @Test
    public void testTimeZoneRequest() throws IOException {
        TimeZoneAPI timeZoneAPI = new TimeZoneAPI();
        assert timeZoneAPI.getTimeZoneForIP("a849gfah") == null;
        assertEquals(TimeZone.getTimeZone("America/Los_Angeles"), timeZoneAPI.getTimeZoneForIP("64.233.185.138"));
    }

    @Test
    public void testSSLFailures() throws IOException {
        NetUtil.get("https://badssl.com/").request();
        assertThrows(SSLException.class, () -> NetUtil.get("https://expired.badssl.com/").request());
        assertThrows(SSLException.class, () -> NetUtil.get("https://wrong.host.badssl.com/").request());
        assertThrows(SSLException.class, () -> NetUtil.get("https://self-signed.badssl.com/").request());
        assertThrows(SSLException.class, () -> NetUtil.get("https://untrusted-root.badssl.com/").request());
    }
}
