package use_case.question_answering;

public interface CourseQuestionInputBoundary {
    void loadCatalog(long courseId);
    void ask(CourseQuestionInputData input);
}
