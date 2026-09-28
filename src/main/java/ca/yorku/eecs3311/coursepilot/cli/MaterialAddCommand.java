package ca.yorku.eecs3311.coursepilot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "add", description = "Import a course material", mixinStandardHelpOptions = true)
public class MaterialAddCommand implements Runnable {
    private final MaterialCommand parent;
    @Option(names = "--course", required = true, description = "Course code") private String courseCode;
    @Option(names = "--file", required = true, description = "Path to PDF, TXT, or Markdown file") private Path file;
    public MaterialAddCommand(MaterialCommand parent) { this.parent = parent; }
    @Override public void run() {
        try {
            Long courseId = parent.facade().getCourseByCode(courseCode).id();
            var result = parent.facade().importMaterial(courseId, file.getFileName().toString(), Files.probeContentType(file), Files.readAllBytes(file));
            System.out.printf("Imported material %d - %s (%d chunks)%n", result.document().id(), result.document().originalFilename(), result.chunkCount());
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("The material file could not be read.", exception);
        }
    }
}
