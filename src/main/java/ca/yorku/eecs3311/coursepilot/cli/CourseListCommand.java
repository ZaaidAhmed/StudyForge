package ca.yorku.eecs3311.coursepilot.cli;

import picocli.CommandLine.Command;

@Command(name = "list", description = "List all courses", mixinStandardHelpOptions = true)
public class CourseListCommand implements Runnable {
    private final CourseCommand parent;

    public CourseListCommand(CourseCommand parent) { this.parent = parent; }

    @Override
    public void run() {
        var courses = parent.facade().listCourses();
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }
        courses.forEach(course -> System.out.printf("%s  %s%n", course.courseCode(), course.name()));
    }
}
