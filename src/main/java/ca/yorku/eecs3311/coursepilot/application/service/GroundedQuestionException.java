package ca.yorku.eecs3311.coursepilot.application.service;

public class GroundedQuestionException extends RuntimeException {
    public GroundedQuestionException(String message) { super(message); }
    public GroundedQuestionException(String message, Throwable cause) { super(message, cause); }
}
