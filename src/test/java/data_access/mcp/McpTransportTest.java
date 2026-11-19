package data_access.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.find_resources.DiscoveryException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class McpTransportTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void acceptsTextOnlyToolResultsInJsonAndFiniteSse(boolean sse) throws Exception {
        ObjectMapper json = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            JsonNode request = json.readTree(exchange.getRequestBody());
            String method = request.path("method").asText();
            if (method.equals("notifications/initialized")) {
                assertEquals("test-session", exchange.getRequestHeaders().getFirst("Mcp-Session-Id"));
                exchange.sendResponseHeaders(202, -1); exchange.close(); return;
            }
            Map<String, Object> result;
            if (method.equals("initialize")) {
                result = Map.of("protocolVersion", "2025-03-26", "capabilities", Map.of("tools", Map.of()));
                exchange.getResponseHeaders().set("Mcp-Session-Id", "test-session");
            }
            else { result = Map.of("content", java.util.List.of(Map.of("type", "text", "text", "{\"items\":[]}"))); }
            String body = json.writeValueAsString(Map.of("jsonrpc", "2.0", "id", request.path("id").asLong(), "result", result));
            if (sse) { body = ": comment\r\nevent: message\r\ndata: " + body + "\r\n\r\n"; }
            exchange.getResponseHeaders().set("Content-Type", sse ? "text/event-stream" : "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            HttpMcpToolGateway tools = new HttpMcpToolGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"), "test", Set.of("list_courses"));
            assertTrue(new McpResourceGateway(tools).listCourses().isEmpty());
        }
        finally { server.stop(0); }
    }
    @Test void malformedProtocolIsNotReportedAsAnEmptyListing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = "{broken".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            HttpMcpToolGateway tools = new HttpMcpToolGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"), "test", Set.of("list_courses"));
            assertEquals(DiscoveryException.Code.INVALID_RESPONSE, assertThrows(DiscoveryException.class, tools::listTools).getCode());
        }
        finally { server.stop(0); }
    }
    @Test void closedServerProducesDisconnectedError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"); server.stop(0);
        HttpMcpToolGateway tools = new HttpMcpToolGateway(endpoint, "test", Set.of("list_courses"));
        assertEquals(DiscoveryException.Code.DISCONNECTED, assertThrows(DiscoveryException.class, tools::listTools).getCode());
    }
}
