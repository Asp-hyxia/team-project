package integration;

import app.AppBuilder;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.question_answering.AnswerPrompt;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.QuestionModelGateway;
import view.CourseQuestionView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void showsDeltasWhileTheWorkerIsStillRunningAndHandlesTheFinalResult(boolean interrupted) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch catalogReady = new CountDownLatch(1);
        CountDownLatch deltaDisplayed = new CountDownLatch(1);
        CountDownLatch finishModel = new CountDownLatch(1);
        CountDownLatch requestFinished = new CountDownLatch(1);
        AtomicReference<CourseQuestionView> demo = new AtomicReference<>();
        QuestionModelGateway model = new QuestionModelGateway() {
            @Override
            public String answer(AnswerPrompt prompt) {
                return fail("Use the streaming overload");
            }

            @Override
            public String answer(AnswerPrompt prompt, Consumer<String> onAnswerDelta) throws CourseQuestionException {
                onAnswerDelta.accept("The ");
                onAnswerDelta.accept("project demo");
                try {
                    if (!finishModel.await(5, TimeUnit.SECONDS) || interrupted) {
                        throw new CourseQuestionException(CourseQuestionException.Code.MODEL_UNAVAILABLE);
                    }
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new CourseQuestionException(CourseQuestionException.Code.MODEL_UNAVAILABLE);
                }
                onAnswerDelta.accept(" is on Friday.");
                return "The project demo is on Friday.";
            }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                CourseQuestionView panel = AppBuilder.buildUc2Demo(worker, model, "UC2 model demo");
                demo.set(panel);
                sendButton(panel).addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) {
                        catalogReady.countDown();
                    }
                });
                JTextArea answer = textArea(panel, false);
                answer.getDocument().addDocumentListener(new DocumentListener() {
                    @Override public void insertUpdate(DocumentEvent event) {
                        if ("The project demo".equals(answer.getText())) {
                            deltaDisplayed.countDown();
                        }
                    }
                    @Override public void removeUpdate(DocumentEvent event) { }
                    @Override public void changedUpdate(DocumentEvent event) { }
                });
            });
            assertTrue(catalogReady.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                CourseQuestionView panel = demo.get();
                JButton send = sendButton(panel);
                send.addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) {
                        requestFinished.countDown();
                    }
                });
                textArea(panel, true).setText("When is the project demo?");
                send.doClick();
            });
            assertTrue(deltaDisplayed.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("The project demo", textArea(demo.get(), false).getText());
                assertFalse(sendButton(demo.get()).isEnabled());
            });
            finishModel.countDown();
            assertTrue(requestFinished.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                String result = textArea(demo.get(), false).getText();
                if (interrupted) {
                    assertEquals("The model is unavailable. Please retry.", result);
                }
                else {
                    assertResult(result, "The project demo is on Friday.");
                }
            });
        }
        finally {
            finishModel.countDown();
            worker.shutdownNow();
        }
    }

    private static String askQuestion(CourseQuestionView panel, boolean selectPage) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JCheckBox page = components(panel, JCheckBox.class).get(0);
            assertTrue(page.getText().contains("Project Timeline"));
            page.setSelected(selectPage);
            JTextArea question = textArea(panel, true);
            question.setText("When is the project demo?");
            sendButton(panel).doClick();
            assertEquals("", question.getText());
        });
        AtomicReference<String> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(textArea(panel, false).getText()));
        return result.get();
    }

    private static JButton sendButton(CourseQuestionView panel) {
        return components(panel, JButton.class).stream().filter(button -> "Send".equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private static JTextArea textArea(CourseQuestionView panel, boolean editable) {
        return components(panel, JTextArea.class).stream().filter(area -> area.isEditable() == editable)
                .findFirst().orElseThrow();
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
