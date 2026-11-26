package data_access.canvas;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import use_case.find_resources.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CanvasResourceGatewayTest {
    private HttpServer server;
    private CanvasResourceGateway gateway;
    private String pages = "[{\"url\":\"project-timeline\",\"title\":\"Project Timeline\"}]";
    private String files = "[{\"id\":2,\"display_name\":\"Notes.pdf\",\"url\":\"https://signed.invalid/?secret=never-return\"}]";
    private int pagesStatus = 200;
    private int filesStatus = 200;
    private String singlePage = "{\"url\":\"timeline\",\"title\":\"Timeline\",\"body\":\"<p>Due Friday.</p>\",\"published\":true}";
    private int singleStatus = 200;
    private String next;
    private final AtomicInteger calls = new AtomicInteger();
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("Bearer test-canvas-only", exchange.getRequestHeaders().getFirst("Authorization"));
            String path = exchange.getRequestURI().getPath();
            int status = 200; String body;
            if (path.equals("/api/v1/courses")) { body = "[{\"id\":1}]"; }
            else if (path.endsWith("favorites/courses")) {
                body = "[{\"id\":1,\"name\":\"Design\",\"course_code\":\"CSC207\",\"workflow_state\":\"available\"},"
                        + "{\"id\":2,\"name\":\"Inactive\",\"workflow_state\":\"available\"}]";
            }
            else if (path.endsWith("/pages")) {
                body = pages; status = pagesStatus;
                if (next != null && !exchange.getRequestURI().getQuery().contains("page=2")) {
                    exchange.getResponseHeaders().add("Link", "<" + next + ">; rel=\"next\"");
                }
            }
            else if (path.endsWith("/pages/timeline")) { body = singlePage; status = singleStatus; }
            else { body = files; status = filesStatus; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start(); gateway = new CanvasResourceGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "test-canvas-only");
    }
    @AfterEach void stop() { server.stop(0); }
    @Test void currentScopeAndCanonicalSourcesExcludeSignedUrls() throws Exception {
        assertEquals(1, gateway.listCourses().size());
        ResourceListing result = gateway.listResources(1);
        assertEquals(2, result.getResources().size());
        assertEquals("https://127.0.0.1/courses/1/files/2", result.getResources().get(1).getSourceUrl().toString());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(DiscoveryException.Code.PERMISSION_DENIED,
                assertThrows(DiscoveryException.class, () -> gateway.listResources(2)).getCode());
    }
    @Test void followsCanvasPagination() throws Exception {
        next = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/courses/1/pages?page=2";
        assertEquals(3, gateway.listResources(1).getResources().size());
    }
    @Test void neverForwardsTokenToExternalPaginationLinks() {
        next = "https://untrusted.invalid/api/v1/courses/1/pages?page=2";
        assertEquals(DiscoveryException.Code.INVALID_RESPONSE, assertThrows(DiscoveryException.class, () -> gateway.listResources(1)).getCode());
        assertEquals(3, calls.get());
    }
    @Test void rejectsPaginationIntoDifferentApiRoute() {
        next = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/users/self/profile";
        assertEquals(DiscoveryException.Code.INVALID_RESPONSE, assertThrows(DiscoveryException.class, () -> gateway.listResources(1)).getCode());
    }
    @Test void directoryDenialIsPartialRatherThanEmptySuccess() throws Exception {
        pagesStatus = 403;
        ResourceListing result = gateway.listResources(1);
        assertEquals(1, result.getResources().size());
        assertTrue(result.getWarnings().get(0).contains("PERMISSION_DENIED"));
    }
    @Test void bothDeniedIsTypedFailure() {
        pagesStatus = 403; filesStatus = 401;
        assertEquals(DiscoveryException.Code.PERMISSION_DENIED, assertThrows(DiscoveryException.class, () -> gateway.listResources(1)).getCode());
    }
    @Test void malformedPayloadIsNotAnEmptySearch() {
        pages = "{\"error\":\"bad\"}";
        assertEquals(DiscoveryException.Code.INVALID_RESPONSE, assertThrows(DiscoveryException.class, () -> gateway.listResources(1)).getCode());
    }
    @Test void lockedAndUnpublishedMetadataIsExcluded() throws Exception {
        pages = "[{\"url\":\"locked\",\"title\":\"Hidden\",\"locked_for_user\":true}, {\"url\":\"draft\",\"title\":\"Draft\",\"published\":false}]";
        files = "[{\"id\":2,\"display_name\":\"Hidden\",\"hidden_for_user\":true}]";
        assertTrue(gateway.listResources(1).getResources().isEmpty());
    }
    @Test void unavailableSectionPreservesOtherResults() throws Exception {
        filesStatus = 503;
        assertTrue(gateway.listResources(1).getWarnings().get(0).contains("UNAVAILABLE"));
    }
    @Test void refusesInsecureRemoteOriginsAndEmbeddedCredentials() {
        for (String origin : List.of("http://example.edu", "https://user@example.edu", "https://example.edu/path", "https://example.edu?token=x")) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasResourceGateway(URI.create(origin), "test"));
        }
    }
    @Test void readsAuthorizedPageBodyAndBuildsCanonicalSource() throws Exception {
        use_case.ask_page.PageContent page = gateway.readPage(1, "timeline");
        assertEquals("<p>Due Friday.</p>", page.text);
        assertEquals("https://127.0.0.1/courses/1/pages/timeline", page.source.toString());
    }

    @Test void rejectsUnauthorizedLockedAndMismatchedPages() {
        assertEquals(use_case.ask_page.AskPageException.Code.PERMISSION_DENIED,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(2, "timeline")).getCode());
        singlePage = "{\"url\":\"timeline\",\"locked_for_user\":true}";
        assertEquals(use_case.ask_page.AskPageException.Code.PERMISSION_DENIED,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(1, "timeline")).getCode());
        singlePage = "{\"url\":\"other\",\"body\":\"Wrong page\"}";
        assertEquals(use_case.ask_page.AskPageException.Code.INVALID_RESPONSE,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(1, "timeline")).getCode());
    }

    @Test void classifiesPageHttpFailuresAndRejectsPathTraversal() {
        singleStatus = 403;
        assertEquals(use_case.ask_page.AskPageException.Code.PERMISSION_DENIED,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(1, "timeline")).getCode());
        singleStatus = 404;
        assertEquals(use_case.ask_page.AskPageException.Code.PAGE_UNAVAILABLE,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(1, "timeline")).getCode());
        int before = calls.get();
        assertEquals(use_case.ask_page.AskPageException.Code.INVALID_INPUT,
                assertThrows(use_case.ask_page.AskPageException.class, () -> gateway.readPage(1, "../private")).getCode());
        assertEquals(before, calls.get());
    }

}
