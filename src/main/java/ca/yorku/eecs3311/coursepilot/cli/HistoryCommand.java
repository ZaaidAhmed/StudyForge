package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.HistoryActivitySummarizer;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "history", description = "Show course learning history", mixinStandardHelpOptions = true)
public class HistoryCommand implements Runnable {
    private final LearningHistoryService history;
    private final StudyAgentFacade facade;

    public HistoryCommand(LearningHistoryService history, StudyAgentFacade facade) {
        this.history = history;
        this.facade = facade;
    }

    @Option(names = "--course", required = true)
    String course;

    @Override
    public void run() {
        var selectedCourse = facade.getCourseByCode(course);
        HistoryActivitySummarizer.summarize(history.list(selectedCourse.id())).forEach(event ->
                System.out.printf("%s %s: %s%n", event.displayTimestamp(), event.displayType(), event.detail()));
    }
}
