package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.SummaryResult;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "summary", description = "Summarize grounded course material", mixinStandardHelpOptions = true)
public class SummaryCommand implements Runnable {
    private final StudyAgentFacade facade;
    public SummaryCommand(StudyAgentFacade facade) { this.facade = facade; }
    @Override public void run() { System.out.println("Use 'coursepilot summary document --help' or 'coursepilot summary topic --help'."); }
    public StudyAgentFacade facade() { return facade; }

    @Component
    @Command(name = "document", description = "Summarize one document", mixinStandardHelpOptions = true)
    public static class Document implements Runnable {
        private final SummaryCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        @Option(names = "--document", required = true) private Long documentId;
        public Document(SummaryCommand parent) { this.parent = parent; }
        @Override public void run() { print(parent.facade().summarizeDocument(parent.facade().getCourseByCode(courseCode).id(), documentId)); }
    }

    @Component
    @Command(name = "topic", description = "Summarize a topic", mixinStandardHelpOptions = true)
    public static class Topic implements Runnable {
        private final SummaryCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        @Option(names = "--topic", required = true) private String topic;
        public Topic(SummaryCommand parent) { this.parent = parent; }
        @Override public void run() { print(parent.facade().summarizeTopic(parent.facade().getCourseByCode(courseCode).id(), topic)); }
    }

    private static void print(SummaryResult result) {
        System.out.println(result.summary());
        if (!result.keyPoints().isEmpty()) { System.out.println("\nKey points:"); result.keyPoints().forEach(point -> System.out.println("- " + point)); }
        System.out.println("\nSources:"); result.sources().forEach(source -> System.out.println(source.sourceId() + " " + source.documentName()));
    }
}
