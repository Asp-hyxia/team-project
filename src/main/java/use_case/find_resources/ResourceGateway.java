package use_case.find_resources;

import entity.Course;
import java.util.List;

/** Implementations must enforce current-user course scope and return metadata only. */
public interface ResourceGateway {
    List<Course> listCourses() throws DiscoveryException;
    ResourceListing listResources(long courseId) throws DiscoveryException;
}
