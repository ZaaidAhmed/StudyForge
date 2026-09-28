package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;

@Command(name = "create", description = "Create a course", mixinStandardHelpOptions = true)
public class CourseCreateCommand implements Runnable {
    private final CourseCommand parent;
    @Option(names = "--code", required = true, description = "Course code") private String code;
    @Option(names = "--name", required = true, description = "Course name") private String name;
    @Option(names = "--description", description = "Optional description") private String description;
    @Option(names = "--exam-date", description = "Optional exam date in ISO format") private LocalDate examDate;

    public CourseCreateCommand(CourseCommand parent) { this.parent = parent; }

    @Override
    public void run() {
        var course = parent.facade().createCourse(new CreateCourseRequest(code, name, description, examDate));
        System.out.printf("Created course %s - %s%n", course.courseCode(), course.name());
    }
}
