package integration;

import app.AppBuilder;
import com.sun.net.httpserver.HttpServer;
import data_access.canvas.CanvasResourceGateway;
import data_access.mcp.HttpMcpToolGateway;
import data_access.mcp.McpPageGateway;
import data_access.mcp.McpResourceGateway;
import infrastructure.mcp.CanvasMcpServer;
import java.awt.Component;
import java.awt.Container;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import use_case.ask_page.AskPageException;
import use_case.ask_page.PageContent;
import static org.junit.jupiter.api.Assertions.*;

class PageWorkflowTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void selectedPageFlowsThroughCanvasMcpAndSwing(boolean directoryUnavailable) throws Exception {
        HttpServer canvas = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        canvas.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body;
            if (path.endsWith("favorites/courses")) {
                body = "[{\"id\":1,\"name\":\"Design\",\"course_code\":\"CSC207\",\"workflow_state\":\"available\"}]";
            }
            else if (path.equals("/api/v1/courses")) { body = "[{\"id\":1}]"; }
            else if (path.endsWith("/pages")) { body = "[{\"url\":\"timeline\",\"title\":\"Timeline\"}]"; }
            else if (path.endsWith("/pages/timeline")) {
                body = "{\"url\":\"timeline\",\"title\":\"Timeline\",\"published\":true,\"body\":\"<p>Due Friday.</p>\"}";
            }
            else { body = "[]"; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            int status = directoryUnavailable && path.endsWith("/pages") ? 404 : 200;
            if (directoryUnavailable && path.endsWith("/files")) { status = 403; }
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        canvas.start();
        CanvasResourceGateway direct = new CanvasResourceGateway(
                URI.create("http://127.0.0.1:" + canvas.getAddress().getPort()), "fixture-token");
        try (CanvasMcpServer server = new CanvasMcpServer(direct, "mcp-fixture-token")) {
            HttpMcpToolGateway tools = new HttpMcpToolGateway(server.getEndpoint(), "mcp-fixture-token",
                    Set.of("list_courses", "list_resources", "read_page"));
            assertEquals(3, tools.listTools().size());
            McpPageGateway pages = new McpPageGateway(tools);
            PageContent page = pages.readPage(1, "timeline");
            assertTrue(page.text.contains("Friday"));
            assertEquals(AskPageException.Code.PERMISSION_DENIED,
                    assertThrows(AskPageException.class, () -> pages.readPage(2, "timeline")).getCode());
            assertEquals(AskPageException.Code.INVALID_INPUT,
                    assertThrows(AskPageException.class, () -> pages.readPage(1, "../private")).getCode());
            AtomicReference<JTabbedPane> ref = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> ref.set(AppBuilder.build(new McpResourceGateway(tools), pages,
                    (question, context) -> {
                        assertEquals(1, context.courseId);
                        assertEquals("timeline", context.slug);
                        return "Friday.";
                    }, Runnable::run)));
            SwingUtilities.invokeAndWait(() -> button(ref.get().getComponentAt(0), "Search resources").doClick());
            SwingUtilities.invokeAndWait(() -> {
                Container resources = (Container) ref.get().getComponentAt(0);
                if (directoryUnavailable) {
                    assertEquals(0, components(resources, JTable.class).get(0).getRowCount());
                    components(resources, JTextField.class).stream()
                            .filter(field -> "Canvas page slug".equals(field.getAccessibleContext().getAccessibleName()))
                            .findFirst().orElseThrow().setText("timeline");
                    button(resources, "Use page slug").doClick();
                }
                else {
                    components(resources, JTable.class).get(0).setRowSelectionInterval(0, 0);
                    button(resources, "Ask about selected page").doClick();
                }
                assertEquals(1, ref.get().getSelectedIndex());
                Container ask = (Container) ref.get().getComponentAt(1);
                components(ask, JTextArea.class).stream().filter(JTextArea::isEditable).findFirst().orElseThrow().setText("When?");
                button(ask, "Ask about selected page").doClick();
            });
            SwingUtilities.invokeAndWait(() -> {
                Container ask = (Container) ref.get().getComponentAt(1);
                String answer = components(ask, JTextArea.class).stream().filter(area -> !area.isEditable()).findFirst().orElseThrow().getText();
                assertTrue(answer.contains("Friday."));
                assertTrue(answer.contains("/courses/1/pages/timeline"));
                assertTrue(button(ask, "Ask about selected page").isEnabled());
                ref.get().setSize(1100, 600);
                layout(ref.get());
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1100, 600, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D graphics = image.createGraphics();
                ref.get().printAll(graphics); graphics.dispose();
                try { javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target/uc3-preview.png").toFile()); }
                catch (java.io.IOException exception) { throw new RuntimeException(exception); }
            });
        }
        finally { canvas.stop(0); }
    }

    private static JButton button(Component root, String text) {
        return components((Container) root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    private static <T> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) { result.add(type.cast(child)); }
            if (child instanceof Container) { result.addAll(components((Container) child, type)); }
        }
        return result;
    }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
