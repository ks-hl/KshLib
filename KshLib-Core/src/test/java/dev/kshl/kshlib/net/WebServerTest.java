package dev.kshl.kshlib.net;

import dev.kshl.kshlib.misc.Timer;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServerTest {

    private TestWebServer webServer;

    @BeforeEach
    public void init() throws InterruptedException {
        webServer = new TestWebServer();
        webServer.start();

        assertTrue(webServer.getPort() > 0);
    }

    private String baseUrl() {
        return "http://localhost:" + webServer.getPort();
    }

    private static JSONObject getJson(String url) throws IOException {
        return Objects.requireNonNull(NetUtil.get(url, false).request().getJSON());
    }

    private static JSONObject postJson(String url, JSONObject body) throws IOException {
        return Objects.requireNonNull(NetUtil.post(url, body.toString(), false).request().getJSON());
    }

    private static HTTPResponseCode postCode(String url, JSONObject body, boolean withTlsFlag) throws IOException {
        return NetUtil.post(url, body.toString(), withTlsFlag).request().getResponseCode();
    }

    @AfterEach
    void tearDown() {
        if (webServer != null) {
            try {
                webServer.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @Timeout(5)
        // seconds
    void testWebServer() throws Exception {
        webServer.registerEndpoint(new Object() {

            @WebServer.Endpoint
            WebServer.Response root(WebServer.Request request) {
                return new WebServer.Response().body(new JSONObject().put("msg", "hello!"));
            }

            @WebServer.Endpoint(value = {"/1", "/2"}, method = HTTPRequestType.POST)
            WebServer.Response oneTwo(WebServer.Request request) throws WebServer.WebException {
                if (request.bodyJSON() != null && request.bodyJSON().has("bad")) {
                    throw new WebServer.WebException(HTTPResponseCode.BAD_REQUEST);
                }
                return new WebServer.Response().body(new JSONObject()
                        .put("success" + request.endpoint().substring(1), true));
            }

            @WebServer.Endpoint("/delay")
            WebServer.Response delay(WebServer.Request request) throws InterruptedException {
                Thread.sleep(250);
                return new WebServer.Response().body(new JSONObject().put("delay", true));
            }

            @WebServer.Endpoint("/ban")
            WebServer.Response ban(WebServer.Request request) {
                return new WebServer.Response().body("");
            }
        });

        final String base = baseUrl();

        // root GET (with and without trailing slash)
        assertEquals("hello!", getJson(base).getString("msg"));
        assertEquals("hello!", getJson(base + "/").getString("msg"));

        // POST /1 and /2 (bad body -> 400; empty body -> success flags)
        assertEquals(
                HTTPResponseCode.BAD_REQUEST,
                postCode(base + "/1", new JSONObject().put("bad", 1), false)
        );
        assertTrue(postJson(base + "/1", new JSONObject()).getBoolean("success1"));

        assertEquals(
                HTTPResponseCode.BAD_REQUEST,
                postCode(base + "/2", new JSONObject().put("bad", 1), true /* matches original default */)
        );
        assertTrue(postJson(base + "/2", new JSONObject()).getBoolean("success2"));

        CountDownLatch bothDone = new CountDownLatch(2);
        @SuppressWarnings("resource")
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Void> f1 = pool.submit(() -> {
            assertTrue(getJson(base + "/delay").optBoolean("delay", false));
            bothDone.countDown();
            return null;
        });

        Future<Void> f2 = pool.submit(() -> {
            Timer t = new Timer();
            assertEquals("hello!", getJson(base + "/").optString("msg", ""));
            assertTrue(t.getMillis() < 50, "Non-delayed request took too long: " + t.getMillis() + "ms");
            bothDone.countDown();
            return null;
        });

        // If adding more concurrent tests, raise rate limit in TestWebServer ctor accordingly.
        assertTrue(bothDone.await(2, TimeUnit.SECONDS), "Concurrent requests did not complete in time");
        f1.get();
        f2.get();
        pool.shutdown();
    }

    @Test
    public void testRateLimiter() throws IOException {
        // RateLimiter is covered more thoroughly in its own tests
        webServer.exhaustRateLimiter();
        assertEquals(HTTPResponseCode.TOO_MANY_REQUESTS, NetUtil.get(baseUrl(), false).request().getResponseCode());
    }

    @Test
    void testAnnotations() throws Exception {
        webServer.registerEndpoint(new Object() {
            @WebServer.Endpoint({"annotation", "annotationCSV"})
            public WebServer.Response annotation(WebServer.Request request) {
                return new WebServer.Response().body("annotation");
            }

            @WebServer.Endpoint(value = "/annotation2/")
            public WebServer.Response annotation2(WebServer.Request request) {
                return new WebServer.Response().body("annotation2");
            }

            @WebServer.Endpoint(value = "error422/")
            public WebServer.Response error422(WebServer.Request request) throws WebServer.WebException {
                throw new WebServer.WebException(HTTPResponseCode.UNPROCESSABLE_ENTITY);
            }
        });

        // bad signatures
        assertThrows(IllegalArgumentException.class, () -> webServer.registerEndpoint(new Object() {
            @WebServer.Endpoint
            public void method(WebServer.Request request) {
            }
        }));
        assertThrows(IllegalArgumentException.class, () -> webServer.registerEndpoint(new Object() {
            @WebServer.Endpoint
            public WebServer.Response method(String wrong) {
                return null;
            }
        }));
        assertThrows(IllegalArgumentException.class, () -> webServer.registerEndpoint(new Object() {
            @WebServer.Endpoint
            public WebServer.Response method(WebServer.Request request, String wrong) {
                return null;
            }
        }));

        final String base = baseUrl();

        assertEquals("annotation", NetUtil.get(base + "/annotation").request().getBody());
        assertEquals("annotation", NetUtil.get(base + "/annotationCSV").request().getBody());
        assertEquals("annotation2", NetUtil.get(base + "/annotation2").request().getBody());
        assertEquals(HTTPResponseCode.UNPROCESSABLE_ENTITY,
                NetUtil.get(base + "/error422").request().getResponseCode());
    }

    private static class TestWebServer extends WebServer {
        TestWebServer() {
            super(0, 0, 1024, new RateLimiter(100, Long.MAX_VALUE), true);
        }

        @Override
        public void print(String msg, Throwable t) {
            warning(msg);
            t.printStackTrace();
        }

        @Override
        public void info(String msg) {
            System.out.println(msg);
        }

        @Override
        public void warning(String msg) {
            System.err.println(msg);
        }

        void exhaustRateLimiter() {
            //noinspection StatementWithEmptyBody
            while (getRateLimiter().check("127.0.0.1")) ;
        }
    }
}
