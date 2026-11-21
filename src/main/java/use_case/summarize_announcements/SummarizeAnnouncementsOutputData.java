package use_case.summarize_announcements;

import entity.Announcement;
import java.util.List;

/** Summary plus original announcements, whose dates and URLs remain authoritative. */
public final class SummarizeAnnouncementsOutputData {
    public final String summary;
    public final List<Announcement> announcements;

    public SummarizeAnnouncementsOutputData(String summary, List<Announcement> announcements) {
        this.summary = summary;
        this.announcements = List.copyOf(announcements);
    }
}
