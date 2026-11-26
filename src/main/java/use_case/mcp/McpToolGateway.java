package use_case.mcp;

import use_case.find_resources.DiscoveryException;
import java.util.List;
import java.util.Map;

/** Shared Java contract: no JSON library, HTTP, Swing or provider SDK types. */
public interface McpToolGateway {
    List<Map<String, Object>> listTools() throws DiscoveryException;
    Map<String, Object> callTool(String name, Map<String, Object> arguments) throws DiscoveryException;
}
