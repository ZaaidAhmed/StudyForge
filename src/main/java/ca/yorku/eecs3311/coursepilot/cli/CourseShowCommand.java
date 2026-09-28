package ca.yorku.eecs3311.coursepilot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "show", description = "Show a course", mixinStandardHelpOptions = true)
public class CourseShowCommand implements Runnable {
    private final CourseCommand parent;
    @Option(names = "--course", required = true, description = "Course code") private String code;

    public CourseShowCommand(CourseCommand parent) { this.parent = parent; }

    @Override
    public void run() {
        var course = parent.facade().getCourseByCode(code);
        System.out.printf("%s - %s%n", course.courseCode(), course.name());
        if (course.description() != null) System.out.println(course.description());
        if (course.examDate() != null) System.out.println("Exam date: " + course.examDate());
    }
}
