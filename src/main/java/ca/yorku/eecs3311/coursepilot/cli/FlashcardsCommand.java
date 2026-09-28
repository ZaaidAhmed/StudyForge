package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "flashcards", description = "Generate and review flashcards", mixinStandardHelpOptions = true)
public class FlashcardsCommand implements Runnable {
    private final StudyAgentFacade facade;
    public FlashcardsCommand(StudyAgentFacade facade) { this.facade = facade; }
    public StudyAgentFacade facade() { return facade; }
    @Override public void run() { System.out.println("Use 'coursepilot flashcards generate --help' or 'flashcards list --help'."); }

    @Component @Command(name = "generate", description = "Generate flashcards from one ready document", mixinStandardHelpOptions = true)
    public static class Generate implements Runnable {
        private final FlashcardsCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        @Option(names = "--document", required = true) private Long documentId;
        @Option(names = "--count", defaultValue = "10") private Integer count;
        public Generate(FlashcardsCommand parent) { this.parent = parent; }
        @Override public void run() { var course = parent.facade().getCourseByCode(courseCode); var result = parent.facade().generateFlashcards(course.id(), documentId, count); System.out.println(result.createdCount() + " flashcards generated."); }
    }

    @Component @Command(name = "list", description = "List persisted course flashcards", mixinStandardHelpOptions = true)
    public static class ListCards implements Runnable {
        private final FlashcardsCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        public ListCards(FlashcardsCommand parent) { this.parent = parent; }
        @Override public void run() { var course = parent.facade().getCourseByCode(courseCode); parent.facade().listFlashcards(course.id()).forEach(card -> System.out.printf("%d. %s%n", card.id(), card.question())); }
    }

    @Component @Command(name = "review", description = "Record a deterministic rating for a flashcard", mixinStandardHelpOptions = true)
    public static class Review implements Runnable {
        private final FlashcardsCommand parent;
        @Option(names = "--course", required = true) private String courseCode;
        @Option(names = "--card", required = true) private Long cardId;
        @Option(names = "--rating", required = true, description = "INCORRECT, HARD, GOOD, or EASY") private String rating;
        public Review(FlashcardsCommand parent) { this.parent = parent; }
        @Override public void run() { var course = parent.facade().getCourseByCode(courseCode); parent.facade().reviewFlashcard(course.id(), cardId, rating); System.out.println("Review saved: " + rating); }
    }
}
