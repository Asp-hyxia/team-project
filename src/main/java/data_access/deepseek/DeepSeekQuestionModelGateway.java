package data_access.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import use_case.question_answering.AnswerPrompt;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.CourseQuestionException.Code;
import use_case.question_answering.QuestionModelGateway;

/** Sends a UC2 text question and its references to DeepSeek Chat Completions. */
public final class DeepSeekQuestionModelGateway implements QuestionModelGateway {
    private static final URI CHAT_COMPLETIONS = URI.create("https://api.deepseek.com/chat/completions");
    private static final String DEFAULT_MODEL = "deepseek-flash";

    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final HttpClient client;
    private final Duration streamTimeout;
    private final ObjectMapper json = new ObjectMapper();

    public static DeepSeekQuestionModelGateway fromEnvironment() throws CourseQuestionException {
        return new DeepSeekQuestionModelGateway(System.getenv("DEEPSEEK_API_KEY"), System.getenv("DEEPSEEK_MODEL"),
                CHAT_COMPLETIONS, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                Duration.ofSeconds(90));
    }

    DeepSeekQuestionModelGateway(String apiKey, String model, URI endpoint, HttpClient client, Duration streamTimeout)
            throws CourseQuestionException {
        if (apiKey == null || !apiKey.strip().matches("[!-~]+")) {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        this.apiKey = apiKey.strip();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.strip();
        this.endpoint = endpoint;
        this.client = client;
        this.streamTimeout = streamTimeout;
    }

    @Override
    public String answer(AnswerPrompt prompt) throws CourseQuestionException {
        return answer(prompt, delta -> { });
    }

    @Override
    public String answer(AnswerPrompt prompt, Consumer<String> onAnswerDelta) throws CourseQuestionException {
        if (prompt == null) {
            throw new CourseQuestionException(Code.INVALID_INPUT);
        }
        ObjectNode body = json.createObjectNode();
        body.put("model", model);
        body.put("stream", true);
        body.putObject("thinking").put("type", "disabled");
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", prompt.getSystemInstruction());
        messages.addObject().put("role", "user").put("content", String.join("\n\n", prompt.getUserParts()));

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                if (response.statusCode() != 200) {
                    throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
                }
                return readStream(stream, onAnswerDelta);
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        catch (IOException exception) {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
    }

    private String readStream(InputStream stream, Consumer<String> onAnswerDelta)
            throws IOException, CourseQuestionException {
        // The HTTP request timeout ends once the response body stream is available.
        CompletableFuture<Void> finished = new CompletableFuture<>();
        finished.orTimeout(streamTimeout.toMillis(), TimeUnit.MILLISECONDS).whenCompleteAsync((unused, failure) -> {
            if (failure != null) {
                try {
                    stream.close();
                }
                catch (IOException ignored) {
                    // The worker still reports the stream timeout.
                }
            }
        });
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder answer = new StringBuilder();
            boolean stopped = false;
            String event;
            while ((event = readEvent(reader)) != null && !"[DONE]".equals(event)) {
                JsonNode choice = readChoice(event);
                String finishReason = choice.path("finish_reason").asText("");
                if (!finishReason.isEmpty()) {
                    if (!"stop".equals(finishReason)) {
                        throw new CourseQuestionException(Code.INVALID_RESPONSE);
                    }
                    stopped = true;
                }
                JsonNode content = choice.path("delta").path("content");
                if (content.isTextual() && !content.asText().isEmpty()) {
                    String delta = content.asText();
                    answer.append(delta);
                    onAnswerDelta.accept(delta);
                }
            }
            String text = answer.toString();
            if (!finished.complete(null)) {
                throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
            }
            if (event != null && stopped && !text.isBlank()) {
                return text;
            }
            throw new CourseQuestionException(Code.INVALID_RESPONSE);
        }
        finally {
            finished.complete(null);
        }
    }

    private static String readEvent(BufferedReader reader) throws IOException {
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append('\n');
                }
                data.append(line.substring("data:".length()).stripLeading());
            }
            else if (line.isEmpty() && data.length() > 0) {
                return data.toString();
            }
        }
        return null;
    }

    private JsonNode readChoice(String event) throws CourseQuestionException {
        try {
            JsonNode chunk = json.readTree(event);
            if (chunk != null && chunk.path("choices").isArray()) {
                return chunk.path("choices").path(0);
            }
        }
        catch (JsonProcessingException exception) {
            throw new CourseQuestionException(Code.INVALID_RESPONSE);
        }
        throw new CourseQuestionException(Code.INVALID_RESPONSE);
    }
}
