package interface_adapter.find_resources;

import use_case.find_resources.FindResourcesInputBoundary;
import use_case.find_resources.FindResourcesInputData;
import java.util.concurrent.Executor;

/** Scheduling is injected; this controller has no Swing or concrete gateway dependency. */
public final class FindResourcesController {
    private final FindResourcesInputBoundary interactor;
    private final Executor worker;
    public FindResourcesController(FindResourcesInputBoundary interactor, Executor worker) {
        this.interactor = interactor; this.worker = worker;
    }
    public void search(Long courseId, String query) {
        FindResourcesInputData input = new FindResourcesInputData(courseId, query);
        worker.execute(() -> interactor.execute(input));
    }
}
