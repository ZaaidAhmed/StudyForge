package ca.yorku.eecs3311.coursepilot.application.error;

public class ApplicationException extends RuntimeException {
    private final ApplicationErrorCode code;

    public ApplicationException(ApplicationErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApplicationException(ApplicationErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApplicationErrorCode code() { return code; }
}
