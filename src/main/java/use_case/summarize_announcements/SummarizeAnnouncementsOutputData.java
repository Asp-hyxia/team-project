package use_case.summarize_announcements;

import entity.Announcement;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Summary, the announcements that produced it, and when Canvas was actually read. */
public final class SummarizeAnnouncementsOutputData {
    public final String summary;
    public final List<Announcement> announcements;
    public final Instant retrievedAt;

    public SummarizeAnnouncementsOutputData(String summary, List<Announcement> announcements, Instant retrievedAt) {
        this.summary = summary;
        this.announcements = List.copyOf(announcements);
        this.retrievedAt = Objects.requireNonNull(retrievedAt);
    }

    /** True when the window contained no announcement, so no summary was generated. */
    public boolean isEmpty() { return announcements.isEmpty(); }
}
