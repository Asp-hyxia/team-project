package integration;

import data_access.DemoResourceGateway;
import data_access.mcp.*;
import infrastructure.mcp.CanvasMcpServer;
import org.junit.jupiter.api.Test;
import use_case.find_resources.*;
import java.net.http.*;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class McpIntegrationTest {
    @Test void realHttpLifecycleAndResourceDiscovery() throws Exception {
        try (CanvasMcpServer server = new CanvasMcpServer(new DemoResourceGateway(), "test-local-only")) {
            HttpMcpToolGateway tools = new HttpMcpToolGateway(server.getEndpoint(), "test-local-only", Set.of("list_courses", "list_resources"));
            assertEquals(2, tools.listTools().size());
            ResourceGateway gateway = new McpResourceGateway(tools);
            assertEquals(1, gateway.listCourses().get(0).getId());
            ResourceListing listing = gateway.listResources(1);
            assertEquals(2, listing.getResources().size());
            assertTrue(listing.getWarnings().get(0).contains("DEMO"));
            assertEquals(DiscoveryException.Code.PERMISSION_DENIED,
                    assertThrows(DiscoveryException.class, () -> gateway.listResources(99)).getCode());
            assertEquals(DiscoveryException.Code.INVALID_INPUT,
                    assertThrows(DiscoveryException.class, () -> tools.callTool("list_resources", Map.of("course_id", "1"))).getCode());
            assertEquals(DiscoveryException.Code.PERMISSION_DENIED,
                    assertThrows(DiscoveryException.class, () -> tools.callTool("submit_assignment", Map.of())).getCode());
        }
    }
    @Test void serverEnforcesAuthenticationOriginAndMethods() throws Exception {
        try (CanvasMcpServer server = new CanvasMcpServer(new DemoResourceGateway(), "test-local-only")) {
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(401, client.send(HttpRequest.newBuilder(server.getEndpoint()).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(405, client.send(HttpRequest.newBuilder(server.getEndpoint()).header("Authorization", "Bearer test-local-only")
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(403, client.send(HttpRequest.newBuilder(server.getEndpoint()).header("Authorization", "Bearer test-local-only")
                    .header("Origin", "https://untrusted.example").POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            HttpMcpToolGateway wrong = new HttpMcpToolGateway(server.getEndpoint(), "wrong", Set.of("list_courses"));
            assertEquals(DiscoveryException.Code.PERMISSION_DENIED, assertThrows(DiscoveryException.class, wrong::listTools).getCode());
        }
    }
    @Test void serverNeverExposesUnallowlistedToolEvenIfClientAllowsIt() throws Exception {
        try (CanvasMcpServer server = new CanvasMcpServer(new DemoResourceGateway(), "test-local-only")) {
            HttpMcpToolGateway tools = new HttpMcpToolGateway(server.getEndpoint(), "test-local-only", Set.of("delete_file", "list_courses"));
            assertEquals(1, tools.listTools().size());
            assertEquals(DiscoveryException.Code.INVALID_INPUT,
                    assertThrows(DiscoveryException.class, () -> tools.callTool("delete_file", Map.of())).getCode());
        }
    }
}
