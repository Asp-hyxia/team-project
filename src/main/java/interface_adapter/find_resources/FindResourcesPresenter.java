package interface_adapter.find_resources;

import use_case.find_resources.*;
import java.util.List;
import java.util.stream.Collectors;

public final class FindResourcesPresenter implements FindResourcesOutputBoundary {
    private final ResourcesViewModel model;
    public FindResourcesPresenter(ResourcesViewModel model) { this.model = model; }
    @Override public void present(FindResourcesOutputData output) {
        List<ResourcesViewModel.CourseOption> courses = output.courses.stream()
                .map(c -> new ResourcesViewModel.CourseOption(c.id, c.name)).collect(Collectors.toList());
        List<ResourcesViewModel.Row> rows = output.resources.stream()
                .map(r -> new ResourcesViewModel.Row(r.courseId, r.resourceId, r.title, r.type, r.course, r.sourceUrl)).collect(Collectors.toList());
        String message;
        if (output.courseSelectionOnly) {
            message = courses.isEmpty() ? "No active favorite/default courses are available." : "Select a course, then search or browse with a blank query.";
        }
        else { message = rows.isEmpty() ? "No matching resources in the returned metadata." : rows.size() + " resource(s) found."; }
        if (!output.warnings.isEmpty()) { message += "\n" + String.join("\n", output.warnings); }
        model.setState(new ResourcesViewModel.State(courses, rows, message));
    }
    @Override public void presentFailure(DiscoveryException.Code code) {
        String message;
        switch (code) {
            case INVALID_INPUT: message = "Select a valid course and use a query of at most 200 characters."; break;
            case PERMISSION_DENIED: message = "Access denied. Check your Canvas credential and current course access."; break;
            case DISCONNECTED: message = "Connection failed. Check the connection and try again."; break;
            case INVALID_RESPONSE: message = "The service returned an unsupported response. Please try again."; break;
            default: message = "The service is unavailable. Please try again later.";
        }
        model.setState(new ResourcesViewModel.State(model.getState().courses, List.of(), message));
    }
}
