package app;

import data_access.UnconfiguredAgentGateway;
import data_access.canvas.CanvasResourceGateway;
import data_access.deepseek.DeepSeekQuestionModelGateway;
import data_access.gemini.GeminiAgentGateway;
import data_access.mcp.HttpMcpToolGateway;
import data_access.mcp.McpPageGateway;
import data_access.mcp.McpResourceGateway;
import infrastructure.mcp.CanvasMcpServer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import use_case.ask_page.AgentGateway;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.QuestionModelGateway;

/** Launch the Canvas application or a UC2 demo. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        ExecutorService worker = Executors.newFixedThreadPool(2);
        if (Arrays.asList(args).contains("--uc2-deepseek")) {
            launchDeepSeekDemo(worker);
            return;
        }
        if (Arrays.asList(args).contains("--uc2-demo")) {
            SwingUtilities.invokeLater(() -> showWindow(AppBuilder.buildUc2Demo(worker), worker, null));
            return;
        }
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
            SwingUtilities.invokeLater(() -> showWindow(
                    AppBuilder.build(new McpResourceGateway(tools), new McpPageGateway(tools), agent, worker),
                    worker, server));
        }
        catch (Exception exception) {
            worker.shutdownNow();
            if (localServer != null) { localServer.close(); }
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Check CANVAS_ORIGIN/CANVAS_TOKEN or MCP_URL/MCP_TOKEN, and GEMINI_API_KEY/GEMINI_MODEL.",
                    "Application configuration", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static void launchDeepSeekDemo(ExecutorService worker) {
        try {
            QuestionModelGateway model = DeepSeekQuestionModelGateway.fromEnvironment();
            SwingUtilities.invokeLater(() -> showWindow(
                    AppBuilder.buildUc2Demo(worker, model, "UC2 demo: simulated course and DeepSeek"),
                    worker, null));
        }
        catch (CourseQuestionException exception) {
            worker.shutdownNow();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Set DEEPSEEK_API_KEY in the run configuration to use --uc2-deepseek.",
                    "DeepSeek configuration", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static void showWindow(JComponent contents, ExecutorService worker, CanvasMcpServer server) {
        JFrame frame = new JFrame("Canvas Academic Assistant");
        frame.setContentPane(contents);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                worker.shutdownNow();
                if (server != null) {
                    server.close();
                }
            }
        });
        frame.setSize(1150, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException(name + " is required."); }
        return value;
    }
}
