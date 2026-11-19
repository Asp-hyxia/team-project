package view;

import interface_adapter.ask_page.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AskPageViewTest {
    @Test void rejectedTaskRestoresUiAndAllowsRetry() throws Exception {
        verifyRecovery(task -> { throw new RejectedExecutionException(); }, "could not start");
    }
    @Test void unexpectedFailureRestoresUiAndAllowsRetry() throws Exception {
        verifyRecovery(Runnable::run, "unexpectedly");
    }
    private void verifyRecovery(Executor executor, String expected) throws Exception {
        AtomicReference<AskPageView> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            AskPageViewModel model = new AskPageViewModel();
            AskPagePresenter presenter = new AskPagePresenter(model);
            AskPageController controller = new AskPageController(input -> {
                throw new IllegalStateException("fixture failure");
            }, executor, presenter);
            ref.set(new AskPageView(controller, model, 1, "timeline"));
            editable(ref.get()).setText("When?");
            button(ref.get()).doClick();
        });
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(button(ref.get()).isEnabled());
            assertTrue(editable(ref.get()).isEnabled());
            assertTrue(components(ref.get(), JTextArea.class).stream().anyMatch(area -> area.getText().contains(expected)));
            button(ref.get()).doClick();
        });
        SwingUtilities.invokeAndWait(() -> assertTrue(button(ref.get()).isEnabled()));
    }
    private static JButton button(Container parent) { return components(parent, JButton.class).stream().filter(b -> "Ask about selected page".equals(b.getText())).findFirst().orElseThrow(); }
    private static JTextArea editable(Container parent) {
        return components(parent, JTextArea.class).stream().filter(JTextArea::isEditable).findFirst().orElseThrow();
    }
    private static <T> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) { result.add(type.cast(child)); }
            if (child instanceof Container) { result.addAll(components((Container) child, type)); }
        }
        return result;
    }
}
