package view;

import interface_adapter.find_resources.*;
import org.junit.jupiter.api.Test;
import use_case.find_resources.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ResourcesViewTest {
    @Test void viewCanSearchAndDisplayAnInertSourceTable() throws Exception {
        ResourcesViewModel model = new ResourcesViewModel(); FindResourcesPresenter presenter = new FindResourcesPresenter(model);
        AtomicReference<FindResourcesInputData> request = new AtomicReference<>();
        AtomicReference<ResourcesView> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            ResourcesView view = new ResourcesView(new FindResourcesController(input -> {
                request.set(input);
                new FindResourcesInteractor(new data_access.DemoResourceGateway(), presenter).execute(input);
            }, Runnable::run), model, "DEMO / synthetic data");
            ref.set(view); view.loadCourses();
        });
        SwingUtilities.invokeAndWait(() -> {
            ResourcesView view = ref.get(); JTable table = component(view, JTable.class);
            assertEquals(0, table.getRowCount());
            JButton search = components(view, JButton.class).stream().filter(b -> b.getText().equals("Search resources")).findFirst().orElseThrow();
            component(view, JTextField.class).setText("project"); search.doClick();
            assertEquals(1L, request.get().getCourseId()); assertEquals("project", request.get().getQuery());
        });
        SwingUtilities.invokeAndWait(() -> {
            ResourcesView view = ref.get(); assertEquals(1, component(view, JTable.class).getRowCount()); view.setSize(1100, 560); layout(view);
            BufferedImage image = new BufferedImage(1100, 560, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); view.printAll(graphics); graphics.dispose();
            try { Files.createDirectories(Path.of("target")); ImageIO.write(image, "png", Path.of("target/resources-preview.png").toFile()); }
            catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        });
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } } }
    private static <T> T component(Container parent, Class<T> type) { return components(parent, type).get(0); }
    private static <T> java.util.List<T> components(Container parent, Class<T> type) {
        java.util.List<T> found = new java.util.ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) { found.add(type.cast(child)); }
            if (child instanceof Container) { found.addAll(components((Container) child, type)); }
        }
        return found;
    }
}
