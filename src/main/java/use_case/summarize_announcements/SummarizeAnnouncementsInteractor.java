package use_case.summarize_announcements;

import entity.Announcement;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Coordinates announcement retrieval, summarization and presentation. */
public final class SummarizeAnnouncementsInteractor implements SummarizeAnnouncementsInputBoundary {
    private final AnnouncementGateway announcementGateway;
    private final AnnouncementSummarizer announcementSummarizer;
    private final SummarizeAnnouncementsOutputBoundary presenter;
    private final Clock clock;

    public SummarizeAnnouncementsInteractor(
            AnnouncementGateway announcementGateway,
            AnnouncementSummarizer announcementSummarizer,
            SummarizeAnnouncementsOutputBoundary presenter) {
        this(announcementGateway, announcementSummarizer, presenter, Clock.systemDefaultZone());
    }

    /** The clock is injected so date windows are deterministic in tests. */
    public SummarizeAnnouncementsInteractor(
            AnnouncementGateway announcementGateway,
            AnnouncementSummarizer announcementSummarizer,
            SummarizeAnnouncementsOutputBoundary presenter,
            Clock clock) {
        this.announcementGateway = announcementGateway;
        this.announcementSummarizer = announcementSummarizer;
        this.presenter = presenter;
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void execute(SummarizeAnnouncementsInputData input) {
        if (input == null || input.startDate == null || input.endDate == null
                || input.courseId <= 0 || input.endDate.isBefore(input.startDate)
                || input.startDate.isAfter(LocalDate.now(clock))) {
            presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_INPUT);
            return;
        }

        List<Announcement> announcements;
        try {
            announcements = announcementGateway.listAnnouncements(input.courseId, input.startDate, input.endDate);
        }
        catch (SummarizeAnnouncementsException exception) {
            presenter.presentFailure(exception.getCode());
            return;
        }

        if (announcements == null) {
            presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_RESPONSE);
            return;
        }
        Instant retrievedAt = clock.instant();
        for (Announcement announcement : announcements) {
            if (announcement == null || announcement.getCourseId() != input.courseId
                    || isOutsideWindow(announcement.getTime(), input.startDate, input.endDate)) {
                presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_RESPONSE);
                return;
            }
        }

        if (announcements.isEmpty()) {
            presenter.present(new SummarizeAnnouncementsOutputData("", announcements, retrievedAt));
            return;
        }

        String summary;
        try {
            summary = announcementSummarizer.summarize(announcements);
        }
        catch (SummarizeAnnouncementsException exception) {
            presenter.presentFailure(exception.getCode());
            return;
        }
        if (summary == null || summary.isBlank()) {
            presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_RESPONSE);
            return;
        }
        presenter.present(new SummarizeAnnouncementsOutputData(summary.strip(), announcements, retrievedAt));
    }

    /** Compares in the clock's zone so a window and a Canvas timestamp agree on "which day". */
    private boolean isOutsideWindow(Instant time, LocalDate start, LocalDate end) {
        LocalDate date = time.atZone(clock.getZone()).toLocalDate();
        return date.isBefore(start) || date.isAfter(end);
    }
}
