package use_case.summarize_announcements;

import entity.Announcement;
import java.util.List;

public interface AnnouncementSummarizer {
    String summarize(List<Announcement> announcements) throws SummarizeAnnouncementsException;
}
