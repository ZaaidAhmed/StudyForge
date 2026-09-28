package ca.yorku.eecs3311.coursepilot.application.service;

public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException(String message) { super(message); }
}
