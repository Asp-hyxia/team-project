package integration;

import app.AppBuilder;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import view.CourseQuestionView;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseQuestionDemoTest {
    @Test
    void sendsAQuestionFromSwingAndShowsTheMockAnswerAndSource() throws Exception {
        AtomicReference<CourseQuestionView> demo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> demo.set(AppBuilder.buildUc2Demo(Runnable::run)));
        SwingUtilities.invokeAndWait(() -> {
            CourseQuestionView panel = demo.get();
            JCheckBox page = components(panel, JCheckBox.class).get(0);
            assertTrue(page.getText().contains("Project Timeline"));
            page.setSelected(true);
            components(panel, JTextArea.class).stream().filter(JTextArea::isEditable).findFirst().orElseThrow()
                    .setText("When is the project demo?");
            components(panel, JButton.class).stream().filter(button -> "Send".equals(button.getText()))
                    .findFirst().orElseThrow().doClick();
        });
        SwingUtilities.invokeAndWait(() -> {
            String result = components(demo.get(), JTextArea.class).stream()
                    .filter(area -> !area.isEditable()).findFirst().orElseThrow().getText();
            assertTrue(result.contains("The project demo is on Friday at 2:00 p.m."));
            assertTrue(result.contains("Sources:\nProject Timeline (demo)"));
            assertTrue(result.contains("https://canvas.example.edu/courses/1/pages/project-timeline"));
        });
    }

    private static <T> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }
            if (child instanceof Container) {
                result.addAll(components((Container) child, type));
            }
        }
        return result;
    }
}
