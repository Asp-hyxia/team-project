package use_case.question_answering;

import entity.CourseResource;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import use_case.find_resources.ResourceListing;
import use_case.question_answering.CourseQuestionException.Code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class CourseQuestionInteractorTest {
    private final FakeContent content = new FakeContent();
    private final CapturedOutput output = new CapturedOutput();

    @Test
    void loadsPageChoicesAndUsesTheReadPageForItsAnswerAndCitation() {
        CourseResource timeline = resource(1, "timeline", CourseResource.Type.PAGE);
        content.pages.put("timeline", new CourseContentGateway.Page(timeline, "The demo is on Friday."));
        content.catalog = List.of(timeline, resource(1, "handout", CourseResource.Type.FILE), timeline);
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content, prompt -> {
            assertEquals(List.of("Question\nWhen is the demo?",
                    "Reference material\ntimeline\nThe demo is on Friday."), prompt.getUserParts());
            return "Friday.";
        }, courseId -> List.of(), output);

        interactor.loadCatalog(1);
        interactor.ask(new CourseQuestionInputData(1, " When is the demo? ", List.of("timeline")));

        assertEquals(List.of("timeline"), content.requests);
        assertEquals(List.of("timeline"), ids(output.pages));
        assertEquals(List.of("Partial catalogue"), output.warnings);
        assertEquals("Friday.", output.answer);
        assertEquals(timeline.getSourceUrl().toString(), output.sources.get(0).url);
        assertNull(output.failure);
    }

    @Test
    void combinesDefaultsAndSelectionsWithoutReadingDuplicates() {
        content.pages.put("default", page(1, "default"));
        content.pages.put("selected", page(1, "selected"));
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content, prompt -> {
            assertEquals(3, prompt.getUserParts().size());
            return "Answer";
        }, courseId -> List.of("default"), output);

        interactor.ask(new CourseQuestionInputData(1, "When?", List.of("default", "selected", "selected")));

        assertEquals(List.of("default", "selected"), content.requests);
        assertEquals(content.requests, ids(output.sources));
        assertNull(output.failure);
    }

    @Test
    void rejectsTooManyReferencesBeforeReadingAnything() {
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content,
                prompt -> fail("Model must not be called"), courseId -> List.of("default"), output);

        interactor.ask(new CourseQuestionInputData(1, "When?", List.of("two", "three", "four")));

        assertEquals(Code.TOO_MANY_REFERENCES, output.failure);
        assertEquals(List.of(), content.requests);
    }

    @Test
    void rejectsBlankQuestionAndMissingReferences() {
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content,
                prompt -> fail("Model must not be called"), courseId -> List.of(), output);

        interactor.ask(new CourseQuestionInputData(1, " ", List.of()));
        assertEquals(Code.INVALID_INPUT, output.failure);
        interactor.ask(new CourseQuestionInputData(1, "When?", List.of()));
        assertEquals(Code.NO_REFERENCES, output.failure);
        assertEquals(List.of(), content.requests);
    }

    @Test
    void blocksUnexpectedContentFromAnotherCourseBeforeCallingTheModel() {
        content.pages.put("timeline", page(2, "timeline"));
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content,
                prompt -> fail("Model must not receive an out-of-course Page"), courseId -> List.of(), output);

        interactor.ask(new CourseQuestionInputData(1, "When?", List.of("timeline")));

        assertEquals(Code.INVALID_RESPONSE, output.failure);
        assertEquals(List.of(), output.sources);
    }

    @Test
    void presentsTheModelFailureWithoutPartialCitations() {
        content.pages.put("timeline", page(1, "timeline"));
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(content, prompt -> {
            throw new CourseQuestionException(Code.MODEL_UNAVAILABLE);
        }, courseId -> List.of("timeline"), output);

        interactor.ask(new CourseQuestionInputData(1, "When?", List.of()));

        assertEquals(Code.MODEL_UNAVAILABLE, output.failure);
        assertNull(output.answer);
        assertEquals(List.of(), output.sources);
    }

    private static CourseContentGateway.Page page(long courseId, String id) {
        return new CourseContentGateway.Page(resource(courseId, id, CourseResource.Type.PAGE), "Reference text");
    }

    private static CourseResource resource(long courseId, String id, CourseResource.Type type) {
        return new CourseResource(id, courseId, id, type,
                URI.create("https://canvas.example.edu/courses/" + courseId + "/resources/" + id));
    }

    private static List<String> ids(List<CourseQuestionOutputBoundary.Reference> sources) {
        List<String> ids = new ArrayList<>();
        for (CourseQuestionOutputBoundary.Reference source : sources) {
            ids.add(source.id);
        }
        return ids;
    }

    private static final class FakeContent implements CourseContentGateway {
        private final Map<String, Page> pages = new LinkedHashMap<>();
        private final List<String> requests = new ArrayList<>();
        private List<CourseResource> catalog = List.of();

        @Override
        public ResourceListing listContent(long courseId) {
            return new ResourceListing(catalog, List.of("Partial catalogue"));
        }

        @Override
        public Page readContent(long courseId, String contentId) throws CourseQuestionException {
            requests.add(contentId);
            Page page = pages.get(contentId);
            if (page == null) {
                throw new CourseQuestionException(Code.CONTENT_UNAVAILABLE);
            }
            return page;
        }
    }

    private static final class CapturedOutput implements CourseQuestionOutputBoundary {
        private List<Reference> pages = List.of();
        private List<String> warnings = List.of();
        private List<Reference> sources = List.of();
        private String answer;
        private Code failure;

        @Override
        public void presentCatalog(List<Reference> pages, List<String> warnings) {
            this.pages = pages;
            this.warnings = warnings;
        }

        @Override
        public void presentAnswer(String answer, List<Reference> sources) {
            this.answer = answer;
            this.sources = sources;
        }

        @Override
        public void presentFailure(Code code) {
            failure = code;
        }
    }
}
