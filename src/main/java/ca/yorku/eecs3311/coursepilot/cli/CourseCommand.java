package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "course", description = "Manage courses", mixinStandardHelpOptions = true)
public class CourseCommand implements Runnable {
    private final StudyAgentFacade facade;

    public CourseCommand(StudyAgentFacade facade) { this.facade = facade; }

    public StudyAgentFacade facade() { return facade; }

    @Override
    public void run() { System.out.println("Use 'studyforge course --help' to manage courses."); }
}
