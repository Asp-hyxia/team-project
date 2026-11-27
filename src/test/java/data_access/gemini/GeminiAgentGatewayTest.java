package data_access.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import use_case.ask_page.AskPageException;
import use_case.ask_page.PageContent;
import static org.junit.jupiter.api.Assertions.*;

class GeminiAgentGatewayTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private JsonNode request;
    private String key;
    private String path;
    private final PageContent page = new PageContent(7, "timeline", "Timeline", "The deadline is Friday.",
            URI.create("https://canvas.example.edu/courses/7/pages/timeline"));

    private GeminiAgentGateway gateway(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().toString();
            key = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            request = json.readTree(exchange.getRequestBody());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new GeminiAgentGateway("fixture-key", "test-model",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), HttpClient.newHttpClient());
    }

    private String response(String finish, List<Map<String, Object>> parts) throws Exception {
        return json.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", finish,
                "content", Map.of("parts", parts)))));
    }

    @AfterEach void close() {
        if (server != null) { server.stop(0); }
    }

    @Test void sendsQuestionAndPageWithCredentialInHeaderAndReadsOnlyAnswer() throws Exception {
        GeminiAgentGateway gateway = gateway(200, response("STOP", List.of(
                Map.of("text", "private reasoning", "thought", true),
                Map.of("text", "Friday."))));
        assertEquals("Friday.", gateway.answer("When is it due?", page));
        assertEquals("fixture-key", key);
        assertEquals("/v1beta/models/test-model:generateContent", path);
        assertTrue(request.path("systemInstruction").toString().contains("untrusted"));
        assertEquals("Question\nWhen is it due?", request.path("contents").path(0).path("parts").path(0).path("text").asText());
        assertEquals("Reference material\n" + page.title + "\n" + page.text,
                request.path("contents").path(0).path("parts").path(1).path("text").asText());
        assertFalse(request.toString().contains("fixture-key"));
    }

    @Test void translatesProviderHttpErrorWithoutLeakingResponseBody() throws Exception {
        AskPageException failure = assertThrows(AskPageException.class,
                () -> gateway(429, "secret provider details").answer("When?", page));
        assertEquals(AskPageException.Code.PROVIDER_RATE_LIMITED, failure.getCode());
        assertFalse(failure.getMessage().contains("secret"));
    }

    @Test void rejectsTruncatedResponse() throws Exception {
        AskPageException failure = assertThrows(AskPageException.class,
                () -> gateway(200, response("MAX_TOKENS", List.of(Map.of("text", "Partial")))).answer("When?", page));
        assertEquals(AskPageException.Code.INVALID_RESPONSE, failure.getCode());
    }

    @Test void rejectsBlockedAndMalformedResponse() throws Exception {
        GeminiAgentGateway blocked = gateway(200, "{}");
        assertEquals(AskPageException.Code.INVALID_RESPONSE,
                assertThrows(AskPageException.class, () -> blocked.answer("When?", page)).getCode());
        server.stop(0);
        GeminiAgentGateway malformed = gateway(200, "not JSON");
        assertEquals(AskPageException.Code.INVALID_RESPONSE,
                assertThrows(AskPageException.class, () -> malformed.answer("When?", page)).getCode());
    }

    @Test void requiresApiKeyAndModel() {
        assertEquals(AskPageException.Code.PROVIDER_NOT_CONFIGURED,
                assertThrows(AskPageException.class, () -> new GeminiAgentGateway("", "test-model")).getCode());
        assertEquals(AskPageException.Code.PROVIDER_NOT_CONFIGURED,
                assertThrows(AskPageException.class, () -> new GeminiAgentGateway("fixture-key", null)).getCode());
        assertEquals(AskPageException.Code.INVALID_INPUT,
                assertThrows(AskPageException.class, () -> new GeminiAgentGateway("fixture-key", "../bad")).getCode());
    }
    @Test void classifiesNonRetryableHttpFailures() throws Exception {
        int[] statuses = {400, 401, 403, 404, 429, 503, 504};
        AskPageException.Code[] codes = {AskPageException.Code.PROVIDER_REQUEST_REJECTED,
                AskPageException.Code.PROVIDER_AUTH_FAILED, AskPageException.Code.PROVIDER_AUTH_FAILED,
                AskPageException.Code.PROVIDER_MODEL_UNAVAILABLE, AskPageException.Code.PROVIDER_RATE_LIMITED,
                AskPageException.Code.PROVIDER_BUSY, AskPageException.Code.PROVIDER_TIMEOUT};
        for (int i = 0; i < statuses.length; i++) {
            GeminiAgentGateway adapter = gateway(statuses[i], "{}");
            assertEquals(codes[i], assertThrows(AskPageException.class, () -> adapter.answer("When?", page)).getCode());
            server.stop(0);
        }
    }

    @Test void retriesTemporaryFailureThenReturnsAnswer() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String success = response("STOP", List.of(Map.of("text", "Friday.")));
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int status = calls.incrementAndGet() == 1 ? 503 : 200;
            byte[] bytes = (status == 200 ? success : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        GeminiAgentGateway adapter = new GeminiAgentGateway("fixture-key", "test-model",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), HttpClient.newHttpClient(), true);
        assertEquals("Friday.", adapter.answer("When?", page));
        assertEquals(2, calls.get());
    }

    @Test void recognizesInvalidApiKeyReportedAsHttp400() throws Exception {
        GeminiAgentGateway adapter = gateway(400,
                "{\"error\":{\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}");
        assertEquals(AskPageException.Code.PROVIDER_AUTH_FAILED,
                assertThrows(AskPageException.class, () -> adapter.answer("When?", page)).getCode());
    }

}
