package use_case.ask_page;

import data_access.UnconfiguredAgentGateway;
import interface_adapter.ask_page.AskPageController;
import interface_adapter.ask_page.AskPagePresenter;
import interface_adapter.ask_page.AskPageViewModel;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AskPageInteractorTest {
    private final PageContent page = new PageContent(7, "timeline", "Timeline", "Demo deadline is Friday.",
            URI.create("https://canvas.example.edu/courses/7/pages/timeline"));
    private final AskPageViewModel model = new AskPageViewModel();
    private final AskPagePresenter presenter = new AskPagePresenter(model);

    @Test void controllerSchedulesWorkAndPresenterDisplaysRetrievedSource() {
        List<Runnable> jobs = new ArrayList<>();
        AskPageInteractor interactor = new AskPageInteractor((course, slug) -> {
            assertEquals(7, course);
            assertEquals("timeline", slug);
            return page;
        }, (question, context) -> {
            assertEquals("When?", question);
            assertSame(page, context);
            return " Friday. ";
        }, presenter);
        new AskPageController(interactor, jobs::add, presenter).ask(7, " timeline ", " When? ");
        assertEquals(1, jobs.size());
        assertEquals("", model.getState().sourceUrl);
        jobs.get(0).run();
        assertEquals("Friday.", model.getState().message);
        assertEquals(page.source.toString(), model.getState().sourceUrl);
        assertEquals(page.title, model.getState().sourceTitle);
    }

    @Test void invalidInputsDoNotCallServices() {
        AskPageInteractor interactor = new AskPageInteractor((c, s) -> {
            fail("No retrieval for invalid input"); return null;
        }, (q, p) -> { fail("No provider call"); return null; }, presenter);
        interactor.execute(null);
        interactor.execute(new AskPageInputData(0, "timeline", "When?"));
        interactor.execute(new AskPageInputData(7, " ", "When?"));
        interactor.execute(new AskPageInputData(7, "timeline", " "));
        interactor.execute(new AskPageInputData(7, "timeline", "x".repeat(4001)));
        assertTrue(model.getState().message.contains("4000"));
    }

    @Test void rejectsWrongCourseOrPageBeforeCallingModel() {
        for (PageContent mismatch : List.of(
                new PageContent(8, "timeline", page.title, page.text, page.source),
                new PageContent(7, "other", page.title, page.text, page.source))) {
            new AskPageInteractor((c, s) -> mismatch, (q, p) -> {
                fail("Mismatched page must not reach model"); return null;
            }, presenter).execute(new AskPageInputData(7, "timeline", "When?"));
            assertTrue(model.getState().message.contains("invalid response"));
        }
    }

    @Test void permissionFailureDoesNotCallModelAndClearsPreviousSource() {
        presenter.present(new AskPageOutputData("Old answer", page.title, page.source.toString()));
        new AskPageInteractor((c, s) -> { throw new AskPageException(AskPageException.Code.PERMISSION_DENIED); },
                (q, p) -> { fail("No model call"); return null; }, presenter)
                .execute(new AskPageInputData(7, "timeline", "When?"));
        assertEquals("", model.getState().sourceUrl);
        assertTrue(model.getState().message.contains("denied"));
    }

    @Test void missingProviderIsExplicit() {
        new AskPageInteractor((c, s) -> page, new UnconfiguredAgentGateway(), presenter)
                .execute(new AskPageInputData(7, "timeline", "When?"));
        assertTrue(model.getState().message.contains("not configured"));
        assertEquals("", model.getState().sourceUrl);
    }

    @Test void emptyAnswerAndProviderFailureAreRecoverable() {
        new AskPageInteractor((c, s) -> page, (q, p) -> " ", presenter)
                .execute(new AskPageInputData(7, "timeline", "When?"));
        assertTrue(model.getState().message.contains("invalid response"));
        new AskPageInteractor((c, s) -> page, (q, p) -> {
            throw new AskPageException(AskPageException.Code.PROVIDER_UNAVAILABLE);
        }, presenter).execute(new AskPageInputData(7, "timeline", "When?"));
        assertTrue(model.getState().message.contains("unavailable"));
    }

    @Test void pageRequiresUsableContentAndHttpsSource() {
        assertThrows(IllegalArgumentException.class, () -> new PageContent(7, "timeline", "Title", " ", page.source));
        assertThrows(IllegalArgumentException.class, () -> new PageContent(7, "timeline", "Title", "Text", URI.create("file:///tmp/page")));
    }
    @Test void allowsCanvasPageSlugsWithPeriodsButRejectsTraversal() {
        assertTrue(PageContent.isValidSlug("week-1.1"));
        assertFalse(PageContent.isValidSlug(".."));
        assertFalse(PageContent.isValidSlug("../private"));
        assertFalse(PageContent.isValidSlug("page?token=x"));
    }

}
