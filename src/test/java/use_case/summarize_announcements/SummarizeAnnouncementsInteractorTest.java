package use_case.summarize_announcements;

import entity.Announcement;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeAnnouncementsInteractorTest {
    private static final long COURSE = 444184;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate START = TODAY.minusDays(7);
    private static final LocalDate END = TODAY.minusDays(1);

    private static final class OutputCapture implements SummarizeAnnouncementsOutputBoundary {
        private SummarizeAnnouncementsOutputData output;
        private SummarizeAnnouncementsException.Code failure;

        @Override
        public void present(SummarizeAnnouncementsOutputData output) { this.output = output; }

        @Override
        public void presentFailure(SummarizeAnnouncementsException.Code code) { this.failure = code; }
    }

    @Test
    void datedAnnouncementsAreSummarizedAndPresentedWithRetrievalTime() {
        Announcement first = announcement("a1", COURSE, START);
        Announcement last = announcement("a2", COURSE, END);
        OutputCapture output = new OutputCapture();

        new SummarizeAnnouncementsInteractor(
                (courseId, startDate, endDate) -> {
                    assertEquals(COURSE, courseId);
                    assertEquals(START, startDate);
                    assertEquals(END, endDate);
                    return List.of(first, last);
                },
                announcements -> {
                    assertEquals(List.of(first, last), announcements);
                    return "  Two updates; check the linked announcements.  ";
                },
                output, CLOCK)
                .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));

        assertNull(output.failure);
        assertNotNull(output.output);
        assertFalse(output.output.isEmpty());
        assertEquals("Two updates; check the linked announcements.", output.output.summary);
        assertEquals(List.of(first, last), output.output.announcements);
        assertEquals(NOW, output.output.retrievedAt);
    }

    @Test
    void emptyWindowPresentsEmptyResultWithoutCallingSummarizer() {
        OutputCapture output = new OutputCapture();

        new SummarizeAnnouncementsInteractor(
                (courseId, startDate, endDate) -> List.of(),
                announcements -> fail("Summarizer must not be called when the window is empty."),
                output, CLOCK)
                .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));

        assertNull(output.failure);
        assertNotNull(output.output);
        assertTrue(output.output.isEmpty());
        assertTrue(output.output.announcements.isEmpty());
        assertEquals("", output.output.summary);
        assertEquals(NOW, output.output.retrievedAt);
    }

    @Test
    void invalidInputsFailBeforeAnyServiceCall() {
        AnnouncementGateway unusedGateway =
                (courseId, startDate, endDate) -> fail("Gateway must not be called for invalid input.");
        AnnouncementSummarizer unusedSummarizer =
                announcements -> fail("Summarizer must not be called for invalid input.");
        List<SummarizeAnnouncementsInputData> invalid = List.of(
                new SummarizeAnnouncementsInputData(COURSE, null, END),
                new SummarizeAnnouncementsInputData(COURSE, START, null),
                new SummarizeAnnouncementsInputData(COURSE, null, null),
                new SummarizeAnnouncementsInputData(0, START, END),
                new SummarizeAnnouncementsInputData(-1, START, END),
                new SummarizeAnnouncementsInputData(COURSE, END, START),
                new SummarizeAnnouncementsInputData(COURSE, TODAY.plusDays(1), TODAY.plusDays(8)));

        for (SummarizeAnnouncementsInputData input : invalid) {
            OutputCapture output = new OutputCapture();
            new SummarizeAnnouncementsInteractor(unusedGateway, unusedSummarizer, output, CLOCK).execute(input);
            assertEquals(SummarizeAnnouncementsException.Code.INVALID_INPUT, output.failure, input.toString());
            assertNull(output.output, input.toString());
        }

        OutputCapture output = new OutputCapture();
        new SummarizeAnnouncementsInteractor(unusedGateway, unusedSummarizer, output, CLOCK).execute(null);
        assertEquals(SummarizeAnnouncementsException.Code.INVALID_INPUT, output.failure);
        assertNull(output.output);
    }

    @Test
    void gatewayFailureCodesReachThePresenterUnchanged() {
        SummarizeAnnouncementsException.Code[] codes = {
                SummarizeAnnouncementsException.Code.PERMISSION_DENIED,
                SummarizeAnnouncementsException.Code.ANNOUNCEMENTS_UNAVAILABLE};
        for (SummarizeAnnouncementsException.Code code : codes) {
            OutputCapture output = new OutputCapture();
            new SummarizeAnnouncementsInteractor(
                    (courseId, startDate, endDate) -> { throw new SummarizeAnnouncementsException(code); },
                    announcements -> "unused",
                    output, CLOCK)
                    .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));
            assertEquals(code, output.failure);
            assertNull(output.output);
        }
    }

    @Test
    void nullGatewayResultIsAnInvalidResponse() {
        OutputCapture output = new OutputCapture();

        new SummarizeAnnouncementsInteractor(
                (courseId, startDate, endDate) -> null,
                announcements -> "unused",
                output, CLOCK)
                .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));

        assertEquals(SummarizeAnnouncementsException.Code.INVALID_RESPONSE, output.failure);
        assertNull(output.output);
    }

    @Test
    void announcementsOutsideTheRequestAreRejected() {
        for (Announcement inconsistent : Arrays.asList(
                null,
                announcement("a1", COURSE + 1, START),
                announcement("a2", COURSE, START.minusDays(1)),
                announcement("a3", COURSE, END.plusDays(1)))) {
            OutputCapture output = new OutputCapture();
            new SummarizeAnnouncementsInteractor(
                    (courseId, startDate, endDate) -> Arrays.asList(inconsistent),
                    announcements -> "unused",
                    output, CLOCK)
                    .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));
            assertEquals(SummarizeAnnouncementsException.Code.INVALID_RESPONSE, output.failure,
                    String.valueOf(inconsistent));
            assertNull(output.output);
        }
    }

    @Test
    void summarizerUnavailableIsReported() {
        OutputCapture output = new OutputCapture();

        new SummarizeAnnouncementsInteractor(
                (courseId, startDate, endDate) -> List.of(announcement("a1", COURSE, START)),
                announcements -> { throw new SummarizeAnnouncementsException(
                        SummarizeAnnouncementsException.Code.SUMMARIZER_UNAVAILABLE); },
                output, CLOCK)
                .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));

        assertEquals(SummarizeAnnouncementsException.Code.SUMMARIZER_UNAVAILABLE, output.failure);
        assertNull(output.output);
    }

    @Test
    void summarizerCodeIsPropagatedUnchanged() {
        OutputCapture output = new OutputCapture();

        new SummarizeAnnouncementsInteractor(
                (courseId, startDate, endDate) -> List.of(announcement("a1", COURSE, START)),
                announcements -> { throw new SummarizeAnnouncementsException(
                        SummarizeAnnouncementsException.Code.PERMISSION_DENIED); },
                output, CLOCK)
                .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));

        assertEquals(SummarizeAnnouncementsException.Code.PERMISSION_DENIED, output.failure);
        assertNull(output.output);
    }

    @Test
    void blankSummariesAreRejected() {
        for (String blank : Arrays.asList(null, "", "   \n ")) {
            OutputCapture output = new OutputCapture();
            new SummarizeAnnouncementsInteractor(
                    (courseId, startDate, endDate) -> List.of(announcement("a1", COURSE, START)),
                    announcements -> blank,
                    output, CLOCK)
                    .execute(new SummarizeAnnouncementsInputData(COURSE, START, END));
            assertEquals(SummarizeAnnouncementsException.Code.INVALID_RESPONSE, output.failure);
            assertNull(output.output);
        }
    }

    private static Announcement announcement(String id, long courseId, LocalDate date) {
        return new Announcement(id, courseId, "CSC207 Software Design",
                date.atTime(15, 0).toInstant(ZoneOffset.UTC),
                "Announcement " + id, "Body of " + id,
                URI.create("https://canvas.example.edu/courses/" + courseId + "/announcements/" + id));
    }
}
