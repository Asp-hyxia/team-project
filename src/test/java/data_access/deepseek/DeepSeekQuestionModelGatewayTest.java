package data_access.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import use_case.question_answering.AnswerPrompt;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.CourseQuestionException.Code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekQuestionModelGatewayTest {
    private static final AnswerPrompt PROMPT = new AnswerPrompt("什么时候演示？",
            List.of("The demo is on Friday.", "The rehearsal is on Thursday."));
    private static final String ANSWER = "{\"choices\":[{\"finish_reason\":\"stop\","
            + "\"message\":{\"content\":\"周五。\",\"reasoning_content\":\"internal reasoning\"}}]}";

    private final AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void closeServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsOrderedReferencesAndReturnsTheFinalAnswer() throws Exception {
        assertEquals("周五。", gateway(200, ANSWER, "deepseek-v4-pro").answer(PROMPT));

        assertEquals("POST", method.get());
        assertEquals("Bearer fixture-key", authorization.get());
        JsonNode body = requestBody.get();
        assertEquals("deepseek-v4-pro", body.path("model").asText());
        assertFalse(body.path("stream").asBoolean(true));
        assertEquals("disabled", body.path("thinking").path("type").asText());
        JsonNode messages = body.path("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).path("role").asText());
        assertEquals(PROMPT.getSystemInstruction(), messages.get(0).path("content").asText());
        assertEquals("user", messages.get(1).path("role").asText());
        assertEquals("Question\n什么时候演示？\n\nReference material\nThe demo is on Friday.\n\n"
                + "Reference material\nThe rehearsal is on Thursday.", messages.get(1).path("content").asText());
        assertFalse(body.toString().contains("fixture-key"));
    }

    @Test
    void usesTheDefaultModelWhenNoneIsConfigured() throws Exception {
        gateway(200, ANSWER, null).answer(PROMPT);

        assertEquals("deepseek-flash", requestBody.get().path("model").asText());
    }

    @Test
    void translatesHttpFailureWithoutReturningProviderDetails() throws Exception {
        DeepSeekQuestionModelGateway gateway = gateway(429, "{\"error\":\"provider details\"}", null);

        CourseQuestionException failure = assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT));
        assertEquals(Code.MODEL_UNAVAILABLE, failure.getCode());
        assertFalse(failure.getMessage().contains("provider details"));
    }

    @Test
    void rejectsTruncatedAnswers() throws Exception {
        DeepSeekQuestionModelGateway gateway = gateway(200, ANSWER.replace("\"stop\"", "\"length\""), null);

        assertEquals(Code.INVALID_RESPONSE,
                assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT)).getCode());
    }

    @Test
    void rejectsMalformedResponses() throws Exception {
        DeepSeekQuestionModelGateway gateway = gateway(200, "{broken", null);

        assertEquals(Code.INVALID_RESPONSE,
                assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT)).getCode());
    }

    @Test
    void requiresAnApiKey() {
        assertEquals(Code.MODEL_UNAVAILABLE, assertThrows(CourseQuestionException.class,
                () -> new DeepSeekQuestionModelGateway(" ", null, null, null)).getCode());
    }

    private DeepSeekQuestionModelGateway gateway(int status, String body, String model) throws Exception {
        ObjectMapper json = new ObjectMapper();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                method.set(exchange.getRequestMethod());
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                requestBody.set(json.readTree(exchange.getRequestBody()));
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            finally {
                exchange.close();
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
        return new DeepSeekQuestionModelGateway("fixture-key", model, endpoint, HttpClient.newHttpClient());
    }
}
