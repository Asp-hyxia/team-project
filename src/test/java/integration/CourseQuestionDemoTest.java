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
import use_case.question_answering.AnswerPrompt;
import view.CourseQuestionView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseQuestionDemoTest {
    @Test
    void sendsAQuestionFromSwingAndShowsTheMockAnswerAndSource() throws Exception {
        AtomicReference<CourseQuestionView> demo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> demo.set(AppBuilder.buildUc2Demo(Runnable::run)));
        assertResult(askQuestion(demo.get(), true), "The project demo is on Friday at 2:00 p.m.");
    }

    @Test
    void sendsTheDefaultPageToTheConfiguredModelAndShowsItsAnswerWithTheSource() throws Exception {
        AtomicReference<CourseQuestionView> demo = new AtomicReference<>();
        AtomicReference<AnswerPrompt> sentPrompt = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> demo.set(AppBuilder.buildUc2Demo(Runnable::run, prompt -> {
            sentPrompt.set(prompt);
            return "Friday at 2 p.m. from the configured model.";
        }, "UC2 model demo")));

        assertResult(askQuestion(demo.get(), false), "Friday at 2 p.m. from the configured model.");
        assertEquals("Question\nWhen is the project demo?", sentPrompt.get().getUserParts().get(0));
        assertTrue(sentPrompt.get().getUserParts().get(1).contains("The project demo is on Friday at 2:00 p.m."));
    }

    private static String askQuestion(CourseQuestionView panel, boolean selectPage) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JCheckBox page = components(panel, JCheckBox.class).get(0);
            assertTrue(page.getText().contains("Project Timeline"));
            page.setSelected(selectPage);
            components(panel, JTextArea.class).stream().filter(JTextArea::isEditable).findFirst().orElseThrow()
                    .setText("When is the project demo?");
            components(panel, JButton.class).stream().filter(button -> "Send".equals(button.getText()))
                    .findFirst().orElseThrow().doClick();
        });
        AtomicReference<String> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(components(panel, JTextArea.class).stream()
                .filter(area -> !area.isEditable()).findFirst().orElseThrow().getText()));
        return result.get();
    }

    private static void assertResult(String result, String expectedAnswer) {
        assertTrue(result.contains(expectedAnswer));
        assertTrue(result.contains("Sources:\nProject Timeline (demo)"));
        assertTrue(result.contains("https://canvas.example.edu/courses/1/pages/project-timeline"));
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
