package ca.yorku.eecs3311.coursepilot.application.service;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String message) { super(message); }
}
