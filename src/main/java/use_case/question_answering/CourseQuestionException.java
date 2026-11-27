package use_case.question_answering;

public final class CourseQuestionException extends Exception {
    public enum Code {
        INVALID_INPUT, NO_REFERENCES, TOO_MANY_REFERENCES, PERMISSION_DENIED,
        CONTENT_UNAVAILABLE, MODEL_UNAVAILABLE, INVALID_RESPONSE, UNEXPECTED_FAILURE
    }

    private final Code code;

    public CourseQuestionException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
