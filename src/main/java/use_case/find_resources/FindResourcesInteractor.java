package use_case.find_resources;

import entity.Course;
import entity.CourseResource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Authorization, title matching, deduplication and ordering are application rules. */
public final class FindResourcesInteractor implements FindResourcesInputBoundary {
    private final ResourceGateway gateway;
    private final FindResourcesOutputBoundary presenter;
    public FindResourcesInteractor(ResourceGateway gateway, FindResourcesOutputBoundary presenter) {
        this.gateway = gateway;
        this.presenter = presenter;
    }
    @Override
    public void execute(FindResourcesInputData input) {
        if (input == null || input.getQuery() == null || input.getQuery().length() > 200
                || (input.getCourseId() != null && input.getCourseId() <= 0)) {
            presenter.presentFailure(DiscoveryException.Code.INVALID_INPUT);
            return;
        }
        try {
            List<Course> courses = gateway.listCourses();
            List<FindResourcesOutputData.CourseData> options = courses.stream()
                    .map(c -> new FindResourcesOutputData.CourseData(c.getId(), c.getName()))
                    .collect(Collectors.toList());
            if (input.getCourseId() == null) {
                presenter.present(new FindResourcesOutputData(options, List.of(), List.of(), true));
                return;
            }
            Course course = courses.stream().filter(c -> c.getId() == input.getCourseId())
                    .findFirst().orElseThrow(() -> new DiscoveryException(DiscoveryException.Code.PERMISSION_DENIED));
            ResourceListing listing = gateway.listResources(course.getId());
            String query = input.getQuery().strip().toLowerCase(Locale.ROOT);
            Set<String> seen = new HashSet<>();
            List<FindResourcesOutputData.ResourceData> rows = new ArrayList<>();
            List<CourseResource> sorted = new ArrayList<>(listing.getResources());
            sorted.sort(Comparator.comparing(CourseResource::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(r -> r.getType().name()).thenComparing(CourseResource::getId));
            for (CourseResource resource : sorted) {
                if (resource.getCourseId() != course.getId()) {
                    throw new DiscoveryException(DiscoveryException.Code.INVALID_RESPONSE);
                }
                if (resource.getTitle().toLowerCase(Locale.ROOT).contains(query)
                        && seen.add(resource.getType() + ":" + resource.getId())) {
                    rows.add(new FindResourcesOutputData.ResourceData(resource.getTitle(),
                            resource.getType().name(), course.getName(), resource.getSourceUrl().toString()));
                }
            }
            presenter.present(new FindResourcesOutputData(options, rows, listing.getWarnings(), false));
        }
        catch (DiscoveryException exception) { presenter.presentFailure(exception.getCode()); }
    }
}
