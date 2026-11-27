package interface_adapter.course_question;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import use_case.question_answering.CourseQuestionException;
import use_case.question_answering.CourseQuestionInputBoundary;
import use_case.question_answering.CourseQuestionInputData;
import use_case.question_answering.CourseQuestionOutputBoundary;

public final class CourseQuestionController {
    private final CourseQuestionInputBoundary interactor;
    private final Executor worker;
    private final CourseQuestionOutputBoundary output;

    public CourseQuestionController(CourseQuestionInputBoundary interactor, Executor worker,
            CourseQuestionOutputBoundary output) {
        this.interactor = interactor;
        this.worker = worker;
        this.output = output;
    }

    public void loadCatalog(long courseId) {
        submit(() -> interactor.loadCatalog(courseId));
    }

    public void ask(long courseId, String question, List<String> selectedContentIds) {
        CourseQuestionInputData input = new CourseQuestionInputData(courseId, question, selectedContentIds);
        submit(() -> interactor.ask(input));
    }

    private void submit(Runnable task) {
        try {
            worker.execute(() -> {
                try {
                    task.run();
                }
                catch (RuntimeException failure) {
                    output.presentFailure(CourseQuestionException.Code.UNEXPECTED_FAILURE);
                }
            });
        }
        catch (RejectedExecutionException failure) {
            output.presentFailure(CourseQuestionException.Code.UNEXPECTED_FAILURE);
        }
    }
}
