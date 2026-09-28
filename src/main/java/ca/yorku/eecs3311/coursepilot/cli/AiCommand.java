package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "ai", description = "Inspect AI integration", mixinStandardHelpOptions = true)
public class AiCommand implements Runnable {
    private final StudyAgentFacade facade;
    public AiCommand(StudyAgentFacade facade) { this.facade = facade; }
    public StudyAgentFacade facade() { return facade; }
    @Override public void run() { System.out.println("Use 'studyforge ai --help' to inspect AI integration."); }
}
