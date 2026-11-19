package use_case.ask_page;

/** A question about a page selected by course ID and Canvas page slug. */
public final class AskPageInputData {
    public final long courseId;
    public final String pageSlug;
    public final String question;

    public AskPageInputData(long courseId, String pageSlug, String question) {
        this.courseId = courseId;
        this.pageSlug = pageSlug;
        this.question = question;
    }
}
