package ca.yorku.eecs3311.coursepilot.application.service;

public class InsightGenerationException extends RuntimeException {
    public InsightGenerationException(String message) { super(message); }
    public InsightGenerationException(String message, Throwable cause) { super(message, cause); }
}
