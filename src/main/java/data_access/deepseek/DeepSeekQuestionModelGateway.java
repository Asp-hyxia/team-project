package data_access.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private final ObjectMapper json = new ObjectMapper();

    public static DeepSeekQuestionModelGateway fromEnvironment() throws CourseQuestionException {
        return new DeepSeekQuestionModelGateway(System.getenv("DEEPSEEK_API_KEY"), System.getenv("DEEPSEEK_MODEL"),
                CHAT_COMPLETIONS, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    DeepSeekQuestionModelGateway(String apiKey, String model, URI endpoint, HttpClient client)
            throws CourseQuestionException {
        if (apiKey == null || !apiKey.strip().matches("[!-~]+")) {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        this.apiKey = apiKey.strip();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.strip();
        this.endpoint = endpoint;
        this.client = client;
    }

    @Override
    public String answer(AnswerPrompt prompt) throws CourseQuestionException {
        if (prompt == null) {
            throw new CourseQuestionException(Code.INVALID_INPUT);
        }
        ObjectNode body = json.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.putObject("thinking").put("type", "disabled");
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", prompt.getSystemInstruction());
        messages.addObject().put("role", "user").put("content", String.join("\n\n", prompt.getUserParts()));

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        catch (IOException exception) {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        if (response.statusCode() != 200) {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }
        return readAnswer(response.body());
    }

    private String readAnswer(String response) throws CourseQuestionException {
        JsonNode result;
        try {
            result = json.readTree(response);
        }
        catch (JsonProcessingException exception) {
            throw new CourseQuestionException(Code.INVALID_RESPONSE);
        }
        if (result == null) {
            throw new CourseQuestionException(Code.INVALID_RESPONSE);
        }
        JsonNode choice = result.path("choices").path(0);
        JsonNode answer = choice.path("message").path("content");
        if (!"stop".equals(choice.path("finish_reason").asText()) || !answer.isTextual()
                || answer.asText().isBlank()) {
            throw new CourseQuestionException(Code.INVALID_RESPONSE);
        }
        return answer.asText();
    }
}
