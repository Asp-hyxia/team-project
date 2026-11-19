package interface_adapter;

import interface_adapter.ask_page.*;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AskPageControllerTest {
    @Test void rejectedTaskIsReportedAndCanBeRetried() {
        AskPageViewModel model = new AskPageViewModel();
        AskPagePresenter presenter = new AskPagePresenter(model);
        AskPageController controller = new AskPageController(input -> fail("Never scheduled"),
                task -> { throw new RejectedExecutionException(); }, presenter);
        controller.ask(1, "page", "Question");
        assertTrue(model.getState().message.contains("could not start"));
        controller.ask(1, "page", "Retry");
        assertTrue(model.getState().message.contains("could not start"));
    }

    @Test void unexpectedFailureIsReportedAndNextRequestCanRun() {
        AskPageViewModel model = new AskPageViewModel();
        AskPagePresenter presenter = new AskPagePresenter(model);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        AskPageController controller = new AskPageController(input -> {
            calls.incrementAndGet(); throw new IllegalStateException("private details");
        }, Runnable::run, presenter);
        controller.ask(1, "page", "Question");
        assertTrue(model.getState().message.contains("unexpectedly"));
        assertFalse(model.getState().message.contains("private"));
        controller.ask(1, "page", "Retry");
        assertEquals(2, calls.get());
    }
}
