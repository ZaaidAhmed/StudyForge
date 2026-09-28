package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.dto.GroundedAnswer;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "ask", description = "Ask a grounded question about one course", mixinStandardHelpOptions = true)
public class AskCommand implements Runnable {
    private final StudyAgentFacade facade;
    @Option(names = "--course", required = true, description = "Course code") private String courseCode;
    @Option(names = "--question", required = true, description = "Question about the course material") private String question;

    public AskCommand(StudyAgentFacade facade) { this.facade = facade; }

    @Override public void run() {
        var course = facade.getCourseByCode(courseCode);
        GroundedAnswer answer = facade.askQuestion(course.id(), question);
        System.out.println("Question:\n" + answer.question());
        System.out.println("\nAnswer:\n" + answer.answer());
        if (!answer.sources().isEmpty()) {
            System.out.println("\nSources:");
            for (int index = 0; index < answer.sources().size(); index++) print(index + 1, answer.sources().get(index));
        }
    }

    private void print(int index, GroundedSource source) {
        String location = source.pageStart() == null ? "Chunk: " + source.chunkIndex()
                : source.pageStart().equals(source.pageEnd()) ? "Page: " + source.pageStart()
                : "Pages: " + source.pageStart() + "-" + source.pageEnd();
        System.out.printf("%d. %s — %s [%s]%n", index, source.documentName(), location, source.sourceId());
    }

    public StudyAgentFacade facade() { return facade; }
}
