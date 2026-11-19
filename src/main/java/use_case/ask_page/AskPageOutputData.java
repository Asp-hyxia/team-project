package use_case.ask_page;

/** Answer and trusted source metadata for presentation. */
public final class AskPageOutputData {
    public final String answer;
    public final String sourceTitle;
    public final String sourceUrl;

    public AskPageOutputData(String answer, String sourceTitle, String sourceUrl) {
        this.answer = answer;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
    }
}
