package data_access.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import use_case.find_resources.DiscoveryException;
import use_case.mcp.McpToolGateway;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static use_case.find_resources.DiscoveryException.Code.*;

/** MCP lifecycle and allowlisted calls. No dependency on Canvas HTTP details. */
public final class HttpMcpToolGateway implements McpToolGateway {
    private static final String VERSION = "2025-03-26";
    private final URI endpoint;
    private final String bearer;
    private final Set<String> allowedTools;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper json = new ObjectMapper();
    private long nextId;
    private boolean initialized;
    private String session;

    public HttpMcpToolGateway(URI endpoint, String bearer, Set<String> allowedTools) {
        boolean local = "http".equals(endpoint.getScheme()) && "127.0.0.1".equals(endpoint.getHost());
        if ((!"https".equals(endpoint.getScheme()) && !local) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getFragment() != null || endpoint.getQuery() != null
                || bearer == null || bearer.isBlank()) { throw new IllegalArgumentException("Invalid MCP connection."); }
        this.endpoint = endpoint; this.bearer = bearer; this.allowedTools = Set.copyOf(allowedTools);
    }
    @Override public synchronized List<Map<String, Object>> listTools() throws DiscoveryException {
        initialize();
        List<Map<String, Object>> tools = new ArrayList<>();
        String cursor = null;
        for (int page = 0; page < 20; page++) {
            JsonNode result = request("tools/list", cursor == null ? Map.of() : Map.of("cursor", cursor));
            if (!result.path("tools").isArray()) { throw new DiscoveryException(INVALID_RESPONSE); }
            for (JsonNode tool : result.path("tools")) {
                if (allowedTools.contains(tool.path("name").asText())) { tools.add(asMap(tool)); }
            }
            cursor = result.path("nextCursor").asText(null);
            if (cursor == null) { return List.copyOf(tools); }
        }
        throw new DiscoveryException(INVALID_RESPONSE);
    }
    @Override public synchronized Map<String, Object> callTool(String name, Map<String, Object> arguments)
            throws DiscoveryException {
        if (!allowedTools.contains(name)) { throw new DiscoveryException(PERMISSION_DENIED); }
        initialize();
        JsonNode result = request("tools/call", Map.of("name", name, "arguments", arguments));
        if (result.path("isError").asBoolean()) {
            DiscoveryException.Code code = UNAVAILABLE;
            try { code = DiscoveryException.Code.valueOf(result.path("content").path(0).path("text").asText()); }
            catch (IllegalArgumentException ignored) { /* Never expose arbitrary server error text. */ }
            throw new DiscoveryException(code);
        }
        JsonNode data = result.get("structuredContent");
        if (data == null) {
            try { data = json.readTree(result.path("content").path(0).path("text").asText()); }
            catch (IOException exception) { throw new DiscoveryException(INVALID_RESPONSE); }
        }
        if (data == null || !data.isObject()) { throw new DiscoveryException(INVALID_RESPONSE); }
        return asMap(data);
    }
    private void initialize() throws DiscoveryException {
        if (!initialized) {
            JsonNode result = request("initialize", Map.of("protocolVersion", VERSION, "capabilities", Map.of(),
                    "clientInfo", Map.of("name", "academic-assistant-java", "version", "1.0")));
            if (!VERSION.equals(result.path("protocolVersion").asText()) || !result.path("capabilities").has("tools")) {
                throw new DiscoveryException(INVALID_RESPONSE);
            }
            send(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"), false);
            initialized = true;
        }
    }
    private JsonNode request(String method, Map<String, Object> params) throws DiscoveryException {
        long id = ++nextId;
        JsonNode response = send(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params), true);
        if (!"2.0".equals(response.path("jsonrpc").asText()) || !response.path("id").isIntegralNumber()
                || response.path("id").asLong() != id) { throw new DiscoveryException(INVALID_RESPONSE); }
        if (response.has("error")) { throw new DiscoveryException(UNAVAILABLE); }
        if (!response.path("result").isObject()) { throw new DiscoveryException(INVALID_RESPONSE); }
        return response.path("result");
    }
    private JsonNode send(Map<String, Object> message, boolean expectsResponse) throws DiscoveryException {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                    .header("Authorization", "Bearer " + bearer).header("MCP-Protocol-Version", VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(message)));
            if (session != null) { request.header("Mcp-Session-Id", session); }
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) { throw new DiscoveryException(PERMISSION_DENIED); }
            if (response.statusCode() < 200 || response.statusCode() >= 300) { throw new DiscoveryException(UNAVAILABLE); }
            session = response.headers().firstValue("Mcp-Session-Id").orElse(session);
            if (!expectsResponse) { return json.createObjectNode(); }
            try {
                if (response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream")) {
                    for (String event : response.body().replace("\r\n", "\n").split("\n\n")) {
                        StringBuilder data = new StringBuilder();
                        for (String line : event.split("\n")) {
                            if (line.startsWith("data:")) { data.append(line.substring(5).stripLeading()).append('\n'); }
                        }
                        if (data.length() > 0) {
                            JsonNode node = json.readTree(data.toString());
                            if (node.path("id").isIntegralNumber() && node.path("id").asLong() == ((Number) message.get("id")).longValue()) { return node; }
                        }
                    }
                    throw new DiscoveryException(INVALID_RESPONSE);
                }
                JsonNode node = json.readTree(response.body());
                if (node == null) { throw new DiscoveryException(INVALID_RESPONSE); }
                return node;
            }
            catch (IOException exception) { throw new DiscoveryException(INVALID_RESPONSE); }
        }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new DiscoveryException(DISCONNECTED); }
        catch (IOException exception) { throw new DiscoveryException(DISCONNECTED); }
    }
    private Map<String, Object> asMap(JsonNode node) { return json.convertValue(node, new TypeReference<Map<String, Object>>() { }); }
}
