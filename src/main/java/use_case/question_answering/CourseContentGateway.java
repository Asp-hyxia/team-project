package use_case.question_answering;

import entity.CourseResource;
import java.util.Objects;
import use_case.find_resources.ResourceListing;

/** Read-only course content contract used by UC2. */
public interface CourseContentGateway {
    ResourceListing listContent(long courseId) throws CourseQuestionException;

    /** Returns the authorized whole Page for the requested course and resource ID. */
    Page readContent(long courseId, String contentId) throws CourseQuestionException;

    final class Page {
        public final CourseResource resource;
        public final String text;

        public Page(CourseResource resource, String text) {
            this.resource = Objects.requireNonNull(resource);
            this.text = Objects.requireNonNull(text);
        }
    }
}
