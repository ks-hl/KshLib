package dev.kshl.kshlib.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.TimeZone;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    public void testHeaders() {
        NetUtil.Request request = new NetUtil.Request(null, HTTPRequestType.GET, false);
        request.headers("Hi1");
        request.appendHeaders("Hi2", "Hi3");
        assertArrayEquals(new String[]{"Hi1","Hi2","Hi3"}, request.getHeaders());
    }

//    @Test
//    @Timeout(value = 5)
//    public void testStream() throws IOException {
//        AtomicInteger counter = new AtomicInteger();
//        NetUtil.get("https://sse.dev/test").streamSubscriber(new SubscriberImpl<String>() {
//            @Override
//            public void onError(Throwable throwable) {
//                throwable.printStackTrace();
//            }
//
//            @Override
//            public void onNext(String s) {
//                int count = counter.getAndIncrement();
//                System.out.println(count + ". " + s);
//                if (count >= 3) close();
//            }
//        }).request();
//        System.out.println("Returned");
//    }
}
