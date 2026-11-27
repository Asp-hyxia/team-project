package use_case.question_answering;

import java.util.List;

public interface CourseQuestionOutputBoundary {
    void presentCatalog(List<Reference> pages, List<String> warnings);
    void presentAnswerDelta(String delta);
    void presentAnswer(String answer, List<Reference> sources);
    void presentFailure(CourseQuestionException.Code code);

    final class Reference {
        public final String id;
        public final String title;
        public final String url;

        public Reference(String id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }
    }
}
