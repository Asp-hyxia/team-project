package use_case.summarize_announcements;

public interface SummarizeAnnouncementsOutputBoundary {
    void present(SummarizeAnnouncementsOutputData output);
    void presentFailure(SummarizeAnnouncementsException.Code code);
}
