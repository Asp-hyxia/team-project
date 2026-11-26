package use_case.summarize_announcements;

import entity.Announcement;
import java.time.LocalDate;
import java.util.List;

/** Loads announcements for one course and inclusive date range. */
public interface AnnouncementGateway {
    List<Announcement> listAnnouncements(long courseId, LocalDate startDate, LocalDate endDate)
            throws SummarizeAnnouncementsException;
}
