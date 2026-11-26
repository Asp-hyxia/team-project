package interface_adapter.ask_page;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import use_case.ask_page.AskPageException;
import use_case.ask_page.AskPageInputBoundary;
import use_case.ask_page.AskPageInputData;
import use_case.ask_page.AskPageOutputBoundary;

/** Runs service work off the UI thread and reports scheduling/execution failures. */
public final class AskPageController {
    private final AskPageInputBoundary interactor;
    private final Executor worker;
    private final AskPageOutputBoundary output;
    private final AtomicBoolean running = new AtomicBoolean();

    public AskPageController(AskPageInputBoundary interactor, Executor worker, AskPageOutputBoundary output) {
        this.interactor = interactor;
        this.worker = worker;
        this.output = output;
    }

    public void ask(long courseId, String slug, String question) {
        if (!running.compareAndSet(false, true)) { return; }
        AskPageInputData input = new AskPageInputData(courseId, slug, question);
        try {
            worker.execute(() -> {
                try { interactor.execute(input); }
                catch (RuntimeException exception) {
                    output.presentFailure(AskPageException.Code.UNEXPECTED_FAILURE);
                }
                finally { running.set(false); }
            });
        }
        catch (RejectedExecutionException exception) {
            running.set(false);
            output.presentFailure(AskPageException.Code.SERVICE_BUSY);
        }
    }
}
