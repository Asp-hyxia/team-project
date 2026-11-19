package use_case.ask_page;

/** Stable application failures, independent of provider SDK exceptions. */
public final class AskPageException extends Exception {
    public enum Code {
        INVALID_INPUT, PERMISSION_DENIED, PAGE_UNAVAILABLE, PROVIDER_NOT_CONFIGURED,
        PROVIDER_UNAVAILABLE, PROVIDER_AUTH_FAILED, PROVIDER_MODEL_UNAVAILABLE,
        PROVIDER_RATE_LIMITED, PROVIDER_BUSY, PROVIDER_TIMEOUT, PROVIDER_REQUEST_REJECTED,
        SERVICE_BUSY, UNEXPECTED_FAILURE, INVALID_RESPONSE
    }
    private final Code code;

    public AskPageException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code getCode() { return code; }
}
