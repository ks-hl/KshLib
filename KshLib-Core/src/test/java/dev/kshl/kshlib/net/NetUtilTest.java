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
    public void testSSLFailures2() throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.google.com/"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThrows(SSLException.class, () -> {
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(new URI("https://wrong.host.badssl.com/"))
                    .build();
            HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        });
    }

    @Test
    public void testSSLFailures() {
        NetUtil.get("https://badssl.com/");
        assertThrows(IOException.class, () -> NetUtil.get("https://expired.badssl.com/"));
        assertThrows(IOException.class, () -> NetUtil.get("https://wrong.host.badssl.com/"));
        assertThrows(IOException.class, () -> NetUtil.get("https://self-signed.badssl.com/"));
        assertThrows(IOException.class, () -> NetUtil.get("https://untrusted-root.badssl.com/"));
        assertThrows(IOException.class, () -> NetUtil.get("https://revoked.badssl.com/"));
        assertThrows(IOException.class, () -> NetUtil.get("https://pinning-test.badssl.com/"));
    }
}
