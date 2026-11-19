package infrastructure.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import entity.CourseResource;
import use_case.find_resources.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small stateless, loopback-only Streamable HTTP MCP server for UC2. */
public final class CanvasMcpServer implements AutoCloseable {
    public static final String VERSION = "2025-03-26";
    private final ResourceGateway resources;
    private final String bearer;
    private final HttpServer server;
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final ObjectMapper json = new ObjectMapper();

    public CanvasMcpServer(ResourceGateway resources, String bearer) throws IOException {
        if (bearer == null || bearer.isBlank()) { throw new IllegalArgumentException("MCP credential required."); }
        this.resources = resources; this.bearer = bearer;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", this::handle);
        server.setExecutor(workers);
        server.start();
    }
    public URI getEndpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"); }
    private void handle(HttpExchange exchange) throws IOException {
        try {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8),
                    ("Bearer " + bearer).getBytes(StandardCharsets.UTF_8))) { send(exchange, 401, Map.of()); return; }
            if (exchange.getRequestHeaders().containsKey("Origin")) { send(exchange, 403, Map.of()); return; }
            if (!"/mcp".equals(exchange.getRequestURI().getPath())) { send(exchange, 404, Map.of()); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, Map.of()); return; }
            byte[] bytes = exchange.getRequestBody().readNBytes(65_537);
            if (bytes.length > 65_536) { send(exchange, 413, Map.of()); return; }
            JsonNode request;
            try { request = json.readTree(bytes); }
            catch (IOException exception) { send(exchange, 200, error(null, -32700, "Invalid JSON.")); return; }
            if (request == null || !request.isObject() || !"2.0".equals(request.path("jsonrpc").asText())
                    || !request.path("method").isTextual()) {
                send(exchange, 200, error(null, -32600, "Invalid request.")); return;
            }
            Object id = request.has("id") ? json.convertValue(request.get("id"), Object.class) : null;
            String method = request.path("method").asText();
            if (!request.has("id")) {
                if ("notifications/initialized".equals(method)) { exchange.sendResponseHeaders(202, -1); }
                else { exchange.sendResponseHeaders(400, -1); }
                return;
            }
            Object result;
            switch (method) {
                case "initialize":
                    result = Map.of("protocolVersion", VERSION, "capabilities", Map.of("tools", Map.of()),
                            "serverInfo", Map.of("name", "academic-assistant-canvas", "version", "1.0")); break;
                case "ping": result = Map.of(); break;
                case "tools/list": result = Map.of("tools", toolDefinitions()); break;
                case "tools/call":
                    try { result = toolResult(call(request.path("params"))); }
                    catch (DiscoveryException exception) {
                        result = Map.of("isError", true, "content", List.of(Map.of("type", "text",
                                "text", exception.getCode().name())));
                    }
                    break;
                default: send(exchange, 200, error(id, -32601, "Method unavailable.")); return;
            }
            Map<String, Object> envelope = new java.util.HashMap<>();
            envelope.put("jsonrpc", "2.0"); envelope.put("id", id); envelope.put("result", result);
            send(exchange, 200, envelope);
        }
        catch (RuntimeException exception) { send(exchange, 500, Map.of("error", "MCP request failed.")); }
        finally { exchange.close(); }
    }
    private Map<String, Object> call(JsonNode params) throws DiscoveryException {
        String name = params.path("name").asText();
        JsonNode args = params.path("arguments");
        if (!args.isObject()) { throw new DiscoveryException(DiscoveryException.Code.INVALID_INPUT); }
        if ("list_courses".equals(name) && args.size() == 0) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (entity.Course course : resources.listCourses()) {
                items.add(Map.of("id", course.getId(), "name", course.getName(), "code", course.getCode()));
            }
            return Map.of("items", items);
        }
        if ("list_resources".equals(name) && args.size() == 1 && args.path("course_id").isIntegralNumber()
                && args.path("course_id").canConvertToLong() && args.path("course_id").asLong() > 0) {
            long courseId = args.path("course_id").asLong();
            if (resources.listCourses().stream().noneMatch(c -> c.getId() == courseId)) {
                throw new DiscoveryException(DiscoveryException.Code.PERMISSION_DENIED);
            }
            ResourceListing listing = resources.listResources(courseId);
            List<Map<String, Object>> items = new ArrayList<>();
            for (CourseResource resource : listing.getResources()) {
                items.add(Map.of("id", resource.getId(), "course_id", resource.getCourseId(), "title", resource.getTitle(),
                        "type", resource.getType().name(), "source_url", resource.getSourceUrl().toString()));
            }
            return Map.of("items", items, "warnings", listing.getWarnings());
        }
        throw new DiscoveryException(DiscoveryException.Code.INVALID_INPUT);
    }
    private Map<String, Object> toolResult(Map<String, Object> data) throws IOException {
        // Text JSON supports protocol 2025-03-26 clients; structuredContent also supports newer hosts.
        return Map.of("isError", false, "structuredContent", data,
                "content", List.of(Map.of("type", "text", "text", json.writeValueAsString(data))));
    }
    public static List<Map<String, Object>> toolDefinitions() {
        return List.of(
                Map.of("name", "list_courses", "description", "List current active favorite/default Canvas courses.",
                        "inputSchema", Map.of("type", "object", "properties", Map.of(), "additionalProperties", false)),
                Map.of("name", "list_resources", "description", "List visible Page/file metadata in a selected course; warnings indicate partial coverage.",
                        "inputSchema", Map.of("type", "object", "properties", Map.of("course_id", Map.of("type", "integer", "minimum", 1)),
                                "required", List.of("course_id"), "additionalProperties", false)));
    }
    private static Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("jsonrpc", "2.0"); result.put("id", id);
        result.put("error", Map.of("code", code, "message", message)); return result;
    }
    private void send(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = json.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
    @Override public void close() { server.stop(0); workers.shutdownNow(); }
}
