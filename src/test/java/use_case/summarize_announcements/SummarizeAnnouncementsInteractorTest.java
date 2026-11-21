package use_case.summarize_announcements;

import entity.Announcement;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeAnnouncementsInteractorTest {
    private static final class OutputCapture implements SummarizeAnnouncementsOutputBoundary {
        private SummarizeAnnouncementsOutputData output;
        private SummarizeAnnouncementsException.Code failure;

        @Override
        public void present(SummarizeAnnouncementsOutputData output) {
            this.output = output;
        }

        @Override
        public void presentFailure(SummarizeAnnouncementsException.Code code) {
            this.failure = code;
        }
    }

    @Test
    void successfulRequestSummarizesAndPresentsAnnouncements() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now().minusDays(1);
        Announcement announcement = new Announcement(
                "announcement-1",
                444184,
                "CSC207 Software Design",
                Instant.parse(startDate + "T15:00:00Z"),
                "Project deadline moved to Friday.",
                "The project deadline has moved to Friday.",
                URI.create("https://canvas.example.edu/courses/444184/discussion_topics/1"));
        OutputCapture output = new OutputCapture();

        SummarizeAnnouncementsInteractor interactor =
                new SummarizeAnnouncementsInteractor(
                        new AnnouncementGateway() {
                            @Override
                            public List<Announcement> listAnnouncements(
                                    long courseId, LocalDate requestedStart, LocalDate requestedEnd) {
                                assertEquals(444184, courseId);
                                assertEquals(startDate, requestedStart);
                                assertEquals(endDate, requestedEnd);
                                return List.of(announcement);
                            }
                        },
                        new AnnouncementSummarizer() {
                            @Override
                            public String summarize(List<Announcement> announcements) {
                                assertEquals(List.of(announcement), announcements);
                                return "Project deadline moved to Friday.";
                            }
                        },
                        output);

        interactor.execute(new SummarizeAnnouncementsInputData(444184, startDate, endDate));

        assertNull(output.failure);
        assertNotNull(output.output);
        assertEquals("Project deadline moved to Friday.", output.output.summary);
        assertEquals(List.of(announcement), output.output.announcements);
        assertEquals(announcement.getTime(), output.output.announcements.get(0).getTime());
        assertEquals(announcement.getSourceUrl(), output.output.announcements.get(0).getSourceUrl());
    }

    @Test
    void noAnnouncementsPresentsEmptyResultWithoutCallingSummarizer() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now().minusDays(1);
        OutputCapture output = new OutputCapture();

        SummarizeAnnouncementsInteractor interactor = new SummarizeAnnouncementsInteractor(
                new AnnouncementGateway() {
                    @Override
                    public List<Announcement> listAnnouncements(
                            long courseId, LocalDate requestedStart, LocalDate requestedEnd) {
                        return List.of();
                    }
                },
                new AnnouncementSummarizer() {
                    @Override
                    public String summarize(List<Announcement> announcements) {
                        fail("Summarizer should not be called when there are no announcements.");
                        return null;
                    }
                },
                output);

        interactor.execute(new SummarizeAnnouncementsInputData(444184, startDate, endDate));
        assertNotNull(output.output);
        assertTrue(output.output.announcements.isEmpty());
        assertEquals("", output.output.summary);
        assertNull(output.failure);



        // TODO: Add assertions that output is present, the announcement list is empty,
        // the summary is empty, and no failure was reported.
    }
}
