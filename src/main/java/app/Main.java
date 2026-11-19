package app;

import data_access.UnconfiguredAgentGateway;
import data_access.canvas.CanvasResourceGateway;
import data_access.gemini.GeminiAgentGateway;
import data_access.mcp.HttpMcpToolGateway;
import data_access.mcp.McpPageGateway;
import data_access.mcp.McpResourceGateway;
import infrastructure.mcp.CanvasMcpServer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import use_case.ask_page.AgentGateway;

/** Launch with Canvas origin/token or an existing compatible MCP endpoint/token. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        ExecutorService worker = Executors.newFixedThreadPool(2);
        CanvasMcpServer localServer = null;
        try {
            String mcpUrl = System.getenv("MCP_URL");
            String mcpToken = System.getenv("MCP_TOKEN");
            URI endpoint;
            if (mcpUrl == null || mcpUrl.isBlank()) {
                CanvasResourceGateway canvas = new CanvasResourceGateway(
                        URI.create(required("CANVAS_ORIGIN")), required("CANVAS_TOKEN"));
                mcpToken = UUID.randomUUID().toString();
                localServer = new CanvasMcpServer(canvas, mcpToken);
                endpoint = localServer.getEndpoint();
            }
            else { endpoint = URI.create(mcpUrl); }
            HttpMcpToolGateway tools = new HttpMcpToolGateway(endpoint, mcpToken,
                    Set.of("list_courses", "list_resources", "read_page"));
            AgentGateway agent = System.getenv("GEMINI_API_KEY") == null || System.getenv("GEMINI_API_KEY").isBlank()
                    ? new UnconfiguredAgentGateway() : GeminiAgentGateway.fromEnvironment();
            CanvasMcpServer server = localServer;
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Canvas Academic Assistant");
                frame.setContentPane(AppBuilder.build(new McpResourceGateway(tools), new McpPageGateway(tools), agent, worker));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosed(WindowEvent event) {
                        worker.shutdownNow();
                        if (server != null) { server.close(); }
                    }
                });
                frame.setSize(1150, 650);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        }
        catch (Exception exception) {
            worker.shutdownNow();
            if (localServer != null) { localServer.close(); }
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Check CANVAS_ORIGIN/CANVAS_TOKEN or MCP_URL/MCP_TOKEN, and GEMINI_API_KEY/GEMINI_MODEL.",
                    "Application configuration", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException(name + " is required."); }
        return value;
    }
}
