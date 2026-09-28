package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "concepts", description = "Extract grounded concepts", mixinStandardHelpOptions = true)
public class ConceptsCommand implements Runnable {
    private final StudyAgentFacade facade;
    public ConceptsCommand(StudyAgentFacade facade) { this.facade = facade; }
    @Override public void run() { System.out.println("Use 'coursepilot concepts extract --help'."); }
    public StudyAgentFacade facade() { return facade; }

    @Component
    @Command(name = "extract", description = "Extract concepts from one document", mixinStandardHelpOptions = true)
    public static class Extract implements Runnable {
        private final ConceptsCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        @Option(names = "--document", required = true) private Long documentId;
        public Extract(ConceptsCommand parent) { this.parent = parent; }
        @Override public void run() { parent.facade().extractConcepts(parent.facade().getCourseByCode(courseCode).id(), documentId).forEach(concept -> { System.out.println(concept.name() + ": " + concept.definition()); concept.sources().forEach(source -> System.out.println("  " + source.sourceId() + " " + source.documentName())); }); }
    }
}
