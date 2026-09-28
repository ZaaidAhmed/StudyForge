package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "material", description = "Manage course materials", mixinStandardHelpOptions = true)
public class MaterialCommand implements Runnable {
    private final StudyAgentFacade facade;
    public MaterialCommand(StudyAgentFacade facade) { this.facade = facade; }
    public StudyAgentFacade facade() { return facade; }
    @Override public void run() { System.out.println("Use 'coursepilot material --help' to manage course materials."); }
}
