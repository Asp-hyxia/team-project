package use_case.summarize_announcements;

import entity.Announcement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Coordinates announcement retrieval, summarization and presentation. */
public final class SummarizeAnnouncementsInteractor implements SummarizeAnnouncementsInputBoundary {
    private final AnnouncementGateway announcementGateway;
    private final AnnouncementSummarizer announcementSummarizer;
    private final SummarizeAnnouncementsOutputBoundary presenter;



    public SummarizeAnnouncementsInteractor(
            AnnouncementGateway announcementGateway,
            AnnouncementSummarizer announcementSummarizer,
            SummarizeAnnouncementsOutputBoundary presenter) {
        this.announcementGateway = announcementGateway;
        this.announcementSummarizer = announcementSummarizer;
        this.presenter = presenter;
    }

    @Override
    public void execute(SummarizeAnnouncementsInputData input) {
        // TODO: Validate input, retrieve announcements, handle an empty result,

        if (input == null || input.startDate == null || input.endDate == null ||
                input.courseId <= 0 || input.endDate.isBefore(input.startDate) ||
                input.startDate.isAfter(LocalDate.now())) {
            presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_INPUT);
            return;
        }

        List<Announcement> unprocessedAnnouncements;
        try {
            unprocessedAnnouncements = announcementGateway.listAnnouncements(input.courseId, input.startDate, input.endDate);

        } catch (SummarizeAnnouncementsException e) {
            presenter.presentFailure(e.getCode());
            return;
        }

        if (unprocessedAnnouncements.isEmpty()) {
            presenter.present(new SummarizeAnnouncementsOutputData("", unprocessedAnnouncements));
            return;
        }
        else {
            // summarize non-empty results, and present success or failure.
            String processedAnnouncements;
            try {
                processedAnnouncements = announcementSummarizer.summarize(unprocessedAnnouncements);


            } catch (SummarizeAnnouncementsException e) {
                presenter.presentFailure(SummarizeAnnouncementsException.Code.SUMMARIZER_UNAVAILABLE);
                return;
            }

            if (processedAnnouncements == null || processedAnnouncements.isBlank()){
                presenter.presentFailure(SummarizeAnnouncementsException.Code.INVALID_RESPONSE);
            }
            else {
                presenter.present(new SummarizeAnnouncementsOutputData(processedAnnouncements, unprocessedAnnouncements));
            }
        }
    }
}
