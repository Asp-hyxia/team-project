package interface_adapter;

import interface_adapter.find_resources.*;
import org.junit.jupiter.api.Test;
import use_case.find_resources.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PresentationTest {
    @Test void controllerSchedulesRatherThanCallingServicesOnCallerThread() {
        List<Runnable> scheduled = new ArrayList<>(); List<FindResourcesInputData> inputs = new ArrayList<>();
        FindResourcesController controller = new FindResourcesController(inputs::add, scheduled::add);
        controller.search(7L, "project"); assertTrue(inputs.isEmpty());
        scheduled.get(0).run(); assertEquals(7, inputs.get(0).getCourseId()); assertEquals("project", inputs.get(0).getQuery());
    }
    @Test void successEmptyPartialAndErrorsAreRenderedWithoutStaleRows() {
        ResourcesViewModel model = new ResourcesViewModel(); FindResourcesPresenter presenter = new FindResourcesPresenter(model);
        List<FindResourcesOutputData.CourseData> courses = List.of(new FindResourcesOutputData.CourseData(7, "Design"));
        presenter.present(new FindResourcesOutputData(courses, List.of(), List.of(), true));
        assertTrue(model.getState().message.contains("Select a course"));
        presenter.present(new FindResourcesOutputData(courses, List.of(new FindResourcesOutputData.ResourceData("Guide", "PAGE", "Design", "https://canvas.example.edu/a")), List.of(), false));
        assertEquals(1, model.getState().rows.size()); assertEquals("Design", model.getState().courses.get(0).toString());
        presenter.present(new FindResourcesOutputData(courses, List.of(), List.of("Partial listing"), false));
        assertTrue(model.getState().message.contains("No matching")); assertTrue(model.getState().message.contains("Partial listing"));
        for (DiscoveryException.Code code : DiscoveryException.Code.values()) {
            presenter.presentFailure(code); assertTrue(model.getState().rows.isEmpty()); assertFalse(model.getState().message.isBlank());
            assertEquals(1, model.getState().courses.size());
        }
        presenter.present(new FindResourcesOutputData(List.of(), List.of(), List.of(), true));
        assertTrue(model.getState().message.contains("No active"));
    }
}
