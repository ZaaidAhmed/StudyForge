package ca.yorku.eecs3311.coursepilot.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import ca.yorku.eecs3311.coursepilot.application.service.CourseNotFoundException;
import ca.yorku.eecs3311.coursepilot.application.service.DuplicateCourseCodeException;
import ca.yorku.eecs3311.coursepilot.application.service.DuplicateDocumentException;
import ca.yorku.eecs3311.coursepilot.application.service.DocumentNotFoundException;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentImportException;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentValidationException;

@Component
@Profile("cli")
public class CliRunner implements CommandLineRunner {
    private final ConfigurableApplicationContext context;
    private final CoursePilotCommand command;

    public CliRunner(ConfigurableApplicationContext context, CoursePilotCommand command) {
        this.context = context;
        this.command = command;
    }

    @Override
    public void run(String... args) {
        CommandLine root = new CommandLine(command);
        CommandLine course = new CommandLine(command.courseCommand());
        course.addSubcommand("list", new CourseListCommand(command.courseCommand()));
        course.addSubcommand("create", new CourseCreateCommand(command.courseCommand()));
        course.addSubcommand("show", new CourseShowCommand(command.courseCommand()));
        root.addSubcommand("course", course);
        CommandLine material = new CommandLine(command.materialCommand());
        material.addSubcommand("list", new MaterialListCommand(command.materialCommand()));
        material.addSubcommand("add", new MaterialAddCommand(command.materialCommand()));
        material.addSubcommand("show", new MaterialShowCommand(command.materialCommand()));
        material.addSubcommand("search", new MaterialSearchCommand(command.materialCommand()));
        root.addSubcommand("material", material);
        CommandLine ai = new CommandLine(command.aiCommand());
        ai.addSubcommand("status", new AiStatusCommand(command.aiCommand()));
        root.addSubcommand("ai", ai);
        root.addSubcommand("ask", command.askCommand());
        CommandLine summary = new CommandLine(command.summaryCommand());
        summary.addSubcommand("document", new SummaryCommand.Document(command.summaryCommand()));
        summary.addSubcommand("topic", new SummaryCommand.Topic(command.summaryCommand()));
        root.addSubcommand("summary", summary);
        CommandLine concepts = new CommandLine(command.conceptsCommand());
        concepts.addSubcommand("extract", new ConceptsCommand.Extract(command.conceptsCommand()));
        root.addSubcommand("concepts", concepts);
        CommandLine flashcards = new CommandLine(command.flashcardsCommand());
        flashcards.addSubcommand("generate", new FlashcardsCommand.Generate(command.flashcardsCommand()));
        flashcards.addSubcommand("list", new FlashcardsCommand.ListCards(command.flashcardsCommand()));
        flashcards.addSubcommand("review", new FlashcardsCommand.Review(command.flashcardsCommand()));
        root.addSubcommand("flashcards", flashcards);
        root.setExecutionExceptionHandler((exception, commandLine, parseResult) -> {
            Throwable cause = exception;
            while (cause.getCause() != null) cause = cause.getCause();
            System.err.println("ERROR: " + (cause.getMessage() == null ? "The command could not be completed." : cause.getMessage()));
            return 1;
        });
        int exitCode;
        try {
            exitCode = root.execute(args);
        } catch (CourseNotFoundException | DuplicateCourseCodeException | DuplicateDocumentException
                 | DocumentNotFoundException | DocumentImportException | DocumentValidationException
                 | IllegalArgumentException exception) {
            System.err.println("ERROR: " + exception.getMessage());
            exitCode = 1;
        } catch (Exception exception) {
            Throwable cause = exception;
            while (cause.getCause() != null) cause = cause.getCause();
            System.err.println("ERROR: " + (cause.getMessage() == null ? "The command could not be completed." : cause.getMessage()));
            exitCode = 1;
        }
        final int commandExitCode = exitCode;
        int springExitCode = org.springframework.boot.SpringApplication.exit(context, () -> commandExitCode);
        System.exit(springExitCode);
    }
}
