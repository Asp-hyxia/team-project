package interface_adapter.summarize_announcements;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import use_case.summarize_announcements.SummarizeAnnouncementsException;
import use_case.summarize_announcements.SummarizeAnnouncementsOutputBoundary;
import use_case.summarize_announcements.SummarizeAnnouncementsOutputData;

/** Maps use case output to display strings; no Swing, no network, no business rules. */
public final class SummarizeAnnouncementsPresenter implements SummarizeAnnouncementsOutputBoundary {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter RETRIEVED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final SummarizeAnnouncementsViewModel model;
    private final ZoneId zone;

    public SummarizeAnnouncementsPresenter(SummarizeAnnouncementsViewModel model) {
        this(model, ZoneId.systemDefault());
    }

    /** Zone injection keeps date formatting deterministic in tests. */
    SummarizeAnnouncementsPresenter(SummarizeAnnouncementsViewModel model, ZoneId zone) {
        this.model = model;
        this.zone = zone;
    }

    @Override
    public void present(SummarizeAnnouncementsOutputData output) {
        List<SummarizeAnnouncementsViewModel.Row> rows = output.announcements.stream()
                .sorted((left, right) -> right.getTime().compareTo(left.getTime()))
                .map(announcement -> new SummarizeAnnouncementsViewModel.Row(
                        announcement.getTitle(),
                        DATE.format(announcement.getTime().atZone(zone).toLocalDate()),
                        announcement.getSourceUrl().toString()))
                .collect(Collectors.toList());
        String message = output.isEmpty()
                ? "No announcements were posted in the selected date range."
                : rows.size() + " announcement(s) found, newest first.";
        model.setState(new SummarizeAnnouncementsViewModel.State(rows, output.summary, message,
                "Retrieved " + RETRIEVED.format(output.retrievedAt.atZone(zone)), false));
    }

    @Override
    public void presentFailure(SummarizeAnnouncementsException.Code code) {
        String message;
        switch (code) {
            case INVALID_INPUT:
                message = "Select a course and a date range that is not in the future.";
                break;
            case PERMISSION_DENIED:
                message = "Canvas denied access to these announcements. Check your current course access.";
                break;
            case ANNOUNCEMENTS_UNAVAILABLE:
                message = "The announcements could not be retrieved. Please try again.";
                break;
            case SUMMARIZER_UNAVAILABLE:
                message = "The summary could not be generated. Please try again.";
                break;
            case SERVICE_BUSY:
                message = "A request is already running. Please wait for it to finish.";
                break;
            case UNEXPECTED_FAILURE:
                message = "The request failed unexpectedly. You can try again.";
                break;
            default:
                message = "The service returned an invalid response. Please try again.";
                break;
        }
        model.setState(new SummarizeAnnouncementsViewModel.State(List.of(), "", message, "", false));
    }
}
