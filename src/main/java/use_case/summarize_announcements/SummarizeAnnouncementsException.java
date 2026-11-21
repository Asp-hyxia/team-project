package use_case.summarize_announcements;

/** Stable UC5 failures, independent of Canvas, MCP and model SDK details. */
public final class SummarizeAnnouncementsException extends Exception {
    public enum Code {
        INVALID_INPUT, PERMISSION_DENIED, ANNOUNCEMENTS_UNAVAILABLE,
        SUMMARIZER_UNAVAILABLE, INVALID_RESPONSE
    }

    private final Code code;

    public SummarizeAnnouncementsException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code getCode() { return code; }
}
