package use_case.find_resources;

import entity.CourseResource;
import java.util.List;

/** Partial discovery is explicit, including when there are zero visible results. */
public final class ResourceListing {
    private final List<CourseResource> resources;
    private final List<String> warnings;
    public ResourceListing(List<CourseResource> resources, List<String> warnings) {
        this.resources = List.copyOf(resources);
        this.warnings = List.copyOf(warnings);
    }
    public List<CourseResource> getResources() { return resources; }
    public List<String> getWarnings() { return warnings; }
}
