package dev.kshl.kshlib.net;

import dev.kshl.kshlib.misc.Timer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebServerTest {
    @Test
    @Timeout(value = 5000L)
    public void testWebServer() throws IOException, ExecutionException, InterruptedException {
        WebServer webServer = new TestWebServer() {
            @Override
            protected void onRequest(String sender, boolean isGlobalLimiter, String endpoint) throws WebException {
                if (endpoint.equals("/ban")) throw new WebException(HTTPResponseCode.FORBIDDEN);
                info(String.format("    Rate Limiter [%s] %s", sender, isGlobalLimiter ? "GLOBAL" : endpoint));
            }

            @Override
            public Response handle(Request request) throws WebException {
                assert request.bodyJSON() != null;
                return switch (request.endpoint()) {
                    case "/" -> new Response().body(new JSONObject().put("msg", "hello!"));
                    case "/1" -> {
                        if (request.bodyJSON().has("bad"))
                            throw new WebException(HTTPResponseCode.INSUFFICIENT_STORAGE);
                        yield new Response().body(new JSONObject().put("success1", true));
                    }
                    case "/2" -> {
                        if (request.bodyJSON().has("bad")) throw new WebException(HTTPResponseCode.CONFLICT);
                        yield new Response().body(new JSONObject().put("success2", true));
                    }
                    case "/delay" -> {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException ignored) {
                        }
                        yield new Response().body(new JSONObject().put("delay", true));
                    }
                    case "/ban" -> new Response().body("");
                    default -> null;
                };
            }
        };
        new Thread(webServer).start();

        while (webServer.getPort() <= 0) //noinspection BusyWait
            Thread.sleep(5);

        assert Objects.requireNonNull(NetUtil.get("http://localhost:" + webServer.getPort(), false).request().getJSON()).getString("msg").equals("hello!");
        assert Objects.requireNonNull(NetUtil.get("http://localhost:" + webServer.getPort() + "/", false).request().getJSON()).getString("msg").equals("hello!");
        assert NetUtil.post("http://localhost:" + webServer.getPort() + "/1", new JSONObject().put("bad", 1).toString(), false).request().getResponseCode().getCode() == 507;
        assert Objects.requireNonNull(NetUtil.post("http://localhost:" + webServer.getPort() + "/1", new JSONObject().toString(), false).request().getJSON()).getBoolean("success1");
        assert NetUtil.post("http://localhost:" + webServer.getPort() + "/2", new JSONObject().put("bad", 1).toString()).request().getResponseCode().getCode() == 409;
        assert Objects.requireNonNull(NetUtil.post("http://localhost:" + webServer.getPort() + "/2", new JSONObject().toString()).request().getJSON()).getBoolean("success2");
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        CompletableFuture<Void> future2 = new CompletableFuture<>();
        new Thread(() -> {
            try {
                assert Objects.requireNonNull(NetUtil.get("http://localhost:" + webServer.getPort() + "/delay").request().getJSON()).optBoolean("delay", false);
                future1.complete(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            Timer timer = new Timer();
            try {
                assert Objects.requireNonNull(NetUtil.get("http://localhost:" + webServer.getPort() + "/").request().getJSON()).optString("msg", "").equals("hello!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assert timer.getMillis() < 50;
            future2.complete(null);
        }).start();
        // If adding more tests, raise rate limit to equal number of tests

        future1.get();
        future2.get();

        assert NetUtil.get("http://localhost:" + webServer.getPort() + "/ban").request().getResponseCode() == HTTPResponseCode.FORBIDDEN;

        assert NetUtil.get("http://localhost:" + webServer.getPort() + "/").request().getResponseCode() == HTTPResponseCode.TOO_MANY_REQUESTS;

        webServer.close();
    }

    @Test
    public void testAnnotations() throws IOException, InterruptedException {
        WebServer webServer = new TestWebServer() {
            @Endpoint(
                    value = "annotation,annotationCSV"
            )
            public Response annotation(Request request) {
                return new Response().body("annotation");
            }
        };
        webServer.registerEndpoint(new Object() {
            @WebServer.Endpoint(value = "/annotation2/")
            public WebServer.Response annotation2(WebServer.Request request) {
                return new WebServer.Response().body("annotation2");
            }

            @WebServer.Endpoint(value = "error422/")
            public WebServer.Response error422(WebServer.Request request) throws WebServer.WebException {
                throw new WebServer.WebException(HTTPResponseCode.UNPROCESSABLE_ENTITY);
            }
        });
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
        new Thread(webServer).start();

        //noinspection LoopConditionNotUpdatedInsideLoop
        while (webServer.getPort() <= 0)
            Thread.onSpinWait();

        assertEquals("annotation", NetUtil.get("http://localhost:" + webServer.getPort() + "/annotation").request().getBody());
        assertEquals("annotation", NetUtil.get("http://localhost:" + webServer.getPort() + "/annotationCSV").request().getBody());
        assertEquals("annotation2", NetUtil.get("http://localhost:" + webServer.getPort() + "/annotation2").request().getBody());
        assertEquals(HTTPResponseCode.UNPROCESSABLE_ENTITY, NetUtil.get("http://localhost:" + webServer.getPort() + "/error422").request().getResponseCode());
    }

    private static class TestWebServer extends WebServer {
        public TestWebServer() {
            super(0, 0, 1024, new RateLimiter(9, Long.MAX_VALUE), true);
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

        @Override
        public Response handle(Request request) throws WebException {
            return null;
        }
    }
}
