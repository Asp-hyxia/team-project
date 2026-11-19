package interface_adapter.find_resources;

import use_case.find_resources.FindResourcesInputBoundary;
import use_case.find_resources.FindResourcesInputData;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import use_case.find_resources.FindResourcesOutputBoundary;
import use_case.find_resources.DiscoveryException;

/** Scheduling is injected; this controller has no Swing or concrete gateway dependency. */
public final class FindResourcesController {
    private final FindResourcesInputBoundary interactor;
    private final Executor worker;
    private final FindResourcesOutputBoundary output;
    public FindResourcesController(FindResourcesInputBoundary interactor, Executor worker) {
        this(interactor, worker, null);
    }
    public FindResourcesController(FindResourcesInputBoundary interactor, Executor worker,
            FindResourcesOutputBoundary output) {
        this.interactor = interactor; this.worker = worker; this.output = output;
    }
    public void search(Long courseId, String query) {
        FindResourcesInputData input = new FindResourcesInputData(courseId, query);
        try {
            worker.execute(() -> {
                try { interactor.execute(input); }
                catch (RuntimeException exception) { reportFailure(exception); }
            });
        }
        catch (RejectedExecutionException exception) { reportFailure(exception); }
    }
    private void reportFailure(RuntimeException exception) {
        if (output == null) { throw exception; }
        output.presentFailure(DiscoveryException.Code.UNAVAILABLE);
    }
}
