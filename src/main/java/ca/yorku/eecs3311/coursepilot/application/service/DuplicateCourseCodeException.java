package ca.yorku.eecs3311.coursepilot.application.service;

public class DuplicateCourseCodeException extends RuntimeException {
    public DuplicateCourseCodeException(String message) { super(message); }
}
