package use_case.summarize_announcements;

import java.time.LocalDate;

/** Course and inclusive date range selected by the student. */
public final class SummarizeAnnouncementsInputData {
    public final long courseId;
    public final LocalDate startDate;
    public final LocalDate endDate;

    public SummarizeAnnouncementsInputData(long courseId, LocalDate startDate, LocalDate endDate) {
        this.courseId = courseId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
