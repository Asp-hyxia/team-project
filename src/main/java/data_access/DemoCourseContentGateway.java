package data_access;

import entity.CourseResource;
import use_case.find_resources.DiscoveryException;
import use_case.find_resources.ResourceListing;
import use_case.question_answering.CourseContentGateway;
import use_case.question_answering.CourseQuestionException;

/** Provides the synthetic UC3 catalogue and a whole Page for the offline demo. */
public final class DemoCourseContentGateway implements CourseContentGateway {
    public static final long COURSE_ID = 1;
    public static final String TIMELINE_ID = "project-timeline";
    private final DemoResourceGateway resources = new DemoResourceGateway();

    @Override
    public ResourceListing listContent(long courseId) throws CourseQuestionException {
        try {
            return resources.listResources(courseId);
        }
        catch (DiscoveryException failure) {
            throw new CourseQuestionException(CourseQuestionException.Code.PERMISSION_DENIED);
        }
    }

    @Override
    public Page readContent(long courseId, String contentId) throws CourseQuestionException {
        CourseResource resource = listContent(courseId).getResources().stream()
                .filter(item -> item.getId().equals(contentId) && item.getType() == CourseResource.Type.PAGE)
                .findFirst()
                .orElseThrow(() -> new CourseQuestionException(CourseQuestionException.Code.CONTENT_UNAVAILABLE));
        return new Page(resource, "The project demo is on Friday at 2:00 p.m.");
    }
}
