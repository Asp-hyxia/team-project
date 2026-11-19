package use_case.find_resources;

import entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FindResourcesInteractorTest {
    private static final Course COURSE = new Course(7, "Software Design", "CSC207");
    private static final class Output implements FindResourcesOutputBoundary {
        FindResourcesOutputData data;
        DiscoveryException.Code error;
        @Override public void present(FindResourcesOutputData value) { data = value; }
        @Override public void presentFailure(DiscoveryException.Code code) { error = code; }
    }
    private static CourseResource resource(String id, long course, String title, CourseResource.Type type) {
        return new CourseResource(id, course, title, type, URI.create("https://canvas.example.edu/courses/" + course + "/pages/" + id));
    }
    private static ResourceGateway gateway(List<CourseResource> resources, List<String> warnings) {
        return new ResourceGateway() {
            @Override public List<Course> listCourses() { return List.of(COURSE); }
            @Override public ResourceListing listResources(long id) { return new ResourceListing(resources, warnings); }
        };
    }
    @Test void loadsCoursesWithoutRetrievingResources() {
        Output output = new Output();
        ResourceGateway gateway = new ResourceGateway() {
            @Override public List<Course> listCourses() { return List.of(COURSE); }
            @Override public ResourceListing listResources(long id) { fail("Must not fetch before selection"); return null; }
        };
        new FindResourcesInteractor(gateway, output).execute(new FindResourcesInputData(null, ""));
        assertTrue(output.data.courseSelectionOnly);
        assertEquals(7, output.data.courses.get(0).id);
        assertEquals("Software Design", output.data.courses.get(0).name);
        assertEquals("CSC207", COURSE.getCode());
    }
    @Test void searchesCaseInsensitivelyDeduplicatesAndOrders() {
        Output output = new Output();
        CourseResource first = resource("b", 7, "Project Timeline", CourseResource.Type.PAGE);
        new FindResourcesInteractor(gateway(List.of(first, first,
                resource("a", 7, "Project Guide", CourseResource.Type.FILE),
                resource("c", 7, "Office Hours", CourseResource.Type.PAGE)), List.of()), output)
                .execute(new FindResourcesInputData(7L, "  pRoJeCt  "));
        assertFalse(output.data.courseSelectionOnly);
        assertEquals(List.of("Project Guide", "Project Timeline"),
                List.of(output.data.resources.get(0).title, output.data.resources.get(1).title));
        assertEquals("FILE", output.data.resources.get(0).type);
        assertEquals("Software Design", output.data.resources.get(0).course);
        assertTrue(output.data.resources.get(0).sourceUrl.startsWith("https://"));
    }
    @Test void sameTitleDifferentTypesAndIdsArePreserved() {
        Output output = new Output();
        new FindResourcesInteractor(gateway(List.of(resource("a", 7, "Guide", CourseResource.Type.PAGE),
                resource("a", 7, "Guide", CourseResource.Type.FILE), resource("b", 7, "Guide", CourseResource.Type.PAGE)), List.of()), output)
                .execute(new FindResourcesInputData(7L, ""));
        assertEquals(3, output.data.resources.size());
    }
    @Test void emptyPartialResultsRemainExplicit() {
        Output output = new Output();
        new FindResourcesInteractor(gateway(List.of(), List.of("Files unavailable")), output)
                .execute(new FindResourcesInputData(7L, "nothing"));
        assertTrue(output.data.resources.isEmpty());
        assertEquals(List.of("Files unavailable"), output.data.warnings);
    }
    @Test void unknownCourseIsDeniedBeforeResourceRead() {
        Output output = new Output();
        new FindResourcesInteractor(gateway(List.of(), List.of()), output).execute(new FindResourcesInputData(8L, ""));
        assertEquals(DiscoveryException.Code.PERMISSION_DENIED, output.error);
    }
    @Test void crossCourseResponseIsRejected() {
        Output output = new Output();
        new FindResourcesInteractor(gateway(List.of(resource("a", 8, "Guide", CourseResource.Type.PAGE)), List.of()), output)
                .execute(new FindResourcesInputData(7L, ""));
        assertEquals(DiscoveryException.Code.INVALID_RESPONSE, output.error);
    }
    @Test void invalidInputsNeverCallGateway() {
        ResourceGateway unused = new ResourceGateway() {
            @Override public List<Course> listCourses() { fail("No network for invalid input"); return null; }
            @Override public ResourceListing listResources(long id) { fail(); return null; }
        };
        List<FindResourcesInputData> inputs = new ArrayList<>();
        inputs.add(null); inputs.add(new FindResourcesInputData(null, null));
        inputs.add(new FindResourcesInputData(0L, "")); inputs.add(new FindResourcesInputData(-1L, ""));
        inputs.add(new FindResourcesInputData(7L, "x".repeat(201)));
        for (FindResourcesInputData input : inputs) {
            Output output = new Output(); new FindResourcesInteractor(unused, output).execute(input);
            assertEquals(DiscoveryException.Code.INVALID_INPUT, output.error);
        }
    }
    @ParameterizedTest @EnumSource(DiscoveryException.Code.class)
    void typedFailuresReachPresenter(DiscoveryException.Code code) {
        Output output = new Output();
        ResourceGateway failing = new ResourceGateway() {
            @Override public List<Course> listCourses() throws DiscoveryException { throw new DiscoveryException(code); }
            @Override public ResourceListing listResources(long id) { return null; }
        };
        new FindResourcesInteractor(failing, output).execute(new FindResourcesInputData(null, ""));
        assertEquals(code, output.error);
        assertEquals(code.name(), new DiscoveryException(code).getMessage());
    }
    @Test void listingAndOutputDefensivelyCopyCollections() {
        List<CourseResource> resources = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        ResourceListing listing = new ResourceListing(resources, warnings); warnings.add("changed");
        assertTrue(listing.getWarnings().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> listing.getResources().add(null));
        FindResourcesOutputData output = new FindResourcesOutputData(List.of(), List.of(), warnings, false);
        warnings.clear(); assertEquals(1, output.warnings.size());
    }
}
