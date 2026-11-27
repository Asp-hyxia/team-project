package data_access.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import use_case.question_answering.AnswerPrompt;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.CourseQuestionException.Code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekQuestionModelGatewayTest {
    private static final AnswerPrompt PROMPT = new AnswerPrompt("什么时候演示？",
            List.of("The demo is on Friday.", "The rehearsal is on Thursday."));
    private static final String FIRST_DELTA =
            "data: {\"choices\":[{\"delta\":{\"content\":\"周\",\"reasoning_content\":\"hidden\"}}]}\n\n";
    private static final String REST_OF_ANSWER = ": keep-alive\r\n\r\n"
            + "data: {\"choices\":[\r\ndata: {\"delta\":{\"content\":\"五。\"}}]}\r\n\r\n"
            + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
            + "data: [DONE]\n\n";

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
    void sendsOrderedReferencesAndEmitsDeltasBeforeTheStreamFinishes() throws Exception {
        CountDownLatch receivedFirstDelta = new CountDownLatch(1);
        DeepSeekQuestionModelGateway gateway = gateway("deepseek-v4-pro", Duration.ofSeconds(10), exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream stream = exchange.getResponseBody();
            stream.write(FIRST_DELTA.getBytes(StandardCharsets.UTF_8));
            stream.flush();
            if (await(receivedFirstDelta)) {
                stream.write(REST_OF_ANSWER.getBytes(StandardCharsets.UTF_8));
            }
        });
        List<String> deltas = new ArrayList<>();
        assertEquals("周五。", gateway.answer(PROMPT, delta -> {
            deltas.add(delta);
            receivedFirstDelta.countDown();
        }));
        assertEquals(List.of("周", "五。"), deltas);

        assertEquals("POST", method.get());
        assertEquals("Bearer fixture-key", authorization.get());
        JsonNode body = requestBody.get();
        assertEquals("deepseek-v4-pro", body.path("model").asText());
        assertTrue(body.path("stream").asBoolean());
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
        gateway(200, FIRST_DELTA + REST_OF_ANSWER, null).answer(PROMPT);

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
        DeepSeekQuestionModelGateway gateway =
                gateway(200, FIRST_DELTA + REST_OF_ANSWER.replace("\"stop\"", "\"length\""), null);

        assertEquals(Code.INVALID_RESPONSE,
                assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT)).getCode());
    }

    @Test
    void rejectsMalformedResponses() throws Exception {
        DeepSeekQuestionModelGateway gateway = gateway(200, "data: {broken\n\n", null);

        assertEquals(Code.INVALID_RESPONSE,
                assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT)).getCode());
    }

    @Test
    void rejectsADisconnectedStreamWithoutAFinalEvent() throws Exception {
        DeepSeekQuestionModelGateway gateway = gateway(200,
                FIRST_DELTA + REST_OF_ANSWER.replace("data: [DONE]\n\n", ""), null);
        List<String> deltas = new ArrayList<>();

        assertEquals(Code.INVALID_RESPONSE,
                assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT, deltas::add)).getCode());
        assertEquals(List.of("周", "五。"), deltas);
    }

    @Test
    @Timeout(10)
    void stopsWaitingWhenTheStreamStallsAfterTheFirstDelta() throws Exception {
        CountDownLatch releaseServer = new CountDownLatch(1);
        DeepSeekQuestionModelGateway gateway = gateway(null, Duration.ofSeconds(1), exchange -> {
            exchange.sendResponseHeaders(200, 0);
            OutputStream stream = exchange.getResponseBody();
            stream.write(FIRST_DELTA.getBytes(StandardCharsets.UTF_8));
            stream.flush();
            await(releaseServer);
        });
        List<String> deltas = new ArrayList<>();
        try {
            assertEquals(Code.MODEL_UNAVAILABLE,
                    assertThrows(CourseQuestionException.class, () -> gateway.answer(PROMPT, deltas::add)).getCode());
            assertEquals(List.of("周"), deltas);
        }
        finally {
            releaseServer.countDown();
        }
    }

    @Test
    void requiresAnApiKey() {
        assertEquals(Code.MODEL_UNAVAILABLE, assertThrows(CourseQuestionException.class,
                () -> new DeepSeekQuestionModelGateway(" ", null, null, null, null)).getCode());
    }

    private DeepSeekQuestionModelGateway gateway(int status, String body, String model) throws Exception {
        return gateway(model, Duration.ofSeconds(10), exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        });
    }

    private DeepSeekQuestionModelGateway gateway(String model, Duration timeout, HttpHandler reply) throws Exception {
        ObjectMapper json = new ObjectMapper();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                method.set(exchange.getRequestMethod());
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                requestBody.set(json.readTree(exchange.getRequestBody()));
                reply.handle(exchange);
            }
            finally {
                exchange.close();
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
        return new DeepSeekQuestionModelGateway("fixture-key", model, endpoint, HttpClient.newHttpClient(), timeout);
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
