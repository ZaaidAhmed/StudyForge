package ca.yorku.eecs3311.coursepilot.infrastructure.document;

public class DocumentImportException extends RuntimeException {
    public DocumentImportException(String message) { super(message); }
    public DocumentImportException(String message, Throwable cause) { super(message, cause); }
}
