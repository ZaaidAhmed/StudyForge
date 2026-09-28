package ca.yorku.eecs3311.coursepilot.infrastructure.document;

public class DocumentValidationException extends RuntimeException {
    public DocumentValidationException(String message) { super(message); }
}
