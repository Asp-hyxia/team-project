package data_access;

import entity.Course;
import entity.CourseResource;
import use_case.find_resources.*;
import java.net.URI;
import java.util.List;

/** Synthetic fixtures only; never mistaken for freshly fetched course information. */
public final class DemoResourceGateway implements ResourceGateway {
    @Override public List<Course> listCourses() { return List.of(new Course(1, "DEMO — Software Design", "DEMO207")); }
    @Override public ResourceListing listResources(long courseId) throws DiscoveryException {
        if (courseId != 1) { throw new DiscoveryException(DiscoveryException.Code.PERMISSION_DENIED); }
        return new ResourceListing(List.of(
                new CourseResource("project-timeline", 1, "Project Timeline (demo)", CourseResource.Type.PAGE,
                        URI.create("https://canvas.example.edu/courses/1/pages/project-timeline")),
                new CourseResource("10", 1, "Clean Architecture Notes (demo).pdf", CourseResource.Type.FILE,
                        URI.create("https://canvas.example.edu/courses/1/files/10"))),
                List.of("DEMO: synthetic resources; these source links are examples."));
    }
}
