package use_case.question_answering;

import java.util.List;

public final class CourseQuestionInputData {
    public final long courseId;
    public final String question;
    public final List<String> selectedContentIds;

    public CourseQuestionInputData(long courseId, String question, List<String> selectedContentIds) {
        this.courseId = courseId;
        this.question = question;
        this.selectedContentIds = List.copyOf(selectedContentIds);
    }
}
