package interface_adapter.summarize_announcements;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import use_case.summarize_announcements.SummarizeAnnouncementsException;
import use_case.summarize_announcements.SummarizeAnnouncementsInputBoundary;
import use_case.summarize_announcements.SummarizeAnnouncementsInputData;
import use_case.summarize_announcements.SummarizeAnnouncementsOutputBoundary;

/** Runs UC5 off the Event Dispatch Thread; never touches Swing or a concrete gateway. */
public final class SummarizeAnnouncementsController {
    private final SummarizeAnnouncementsInputBoundary interactor;
    private final Executor worker;
    private final SummarizeAnnouncementsOutputBoundary output;
    private final AtomicBoolean running = new AtomicBoolean();

    public SummarizeAnnouncementsController(SummarizeAnnouncementsInputBoundary interactor, Executor worker,
            SummarizeAnnouncementsOutputBoundary output) {
        this.interactor = interactor;
        this.worker = worker;
        this.output = output;
    }

    /** Silently ignores a second request while one is in flight; the ViewModel keeps the UI disabled. */
    public void summarize(long courseId, LocalDate startDate, LocalDate endDate) {
        if (!running.compareAndSet(false, true)) { return; }
        SummarizeAnnouncementsInputData input = new SummarizeAnnouncementsInputData(courseId, startDate, endDate);
        try {
            worker.execute(() -> {
                try { interactor.execute(input); }
                catch (RuntimeException exception) {
                    output.presentFailure(SummarizeAnnouncementsException.Code.UNEXPECTED_FAILURE);
                }
                finally { running.set(false); }
            });
        }
        catch (RejectedExecutionException exception) {
            running.set(false);
            output.presentFailure(SummarizeAnnouncementsException.Code.SERVICE_BUSY);
        }
    }
}
