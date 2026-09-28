package ca.yorku.eecs3311.coursepilot.application.service;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) { super(message); }
}
