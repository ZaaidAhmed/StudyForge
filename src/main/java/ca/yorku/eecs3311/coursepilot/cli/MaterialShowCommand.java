package ca.yorku.eecs3311.coursepilot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "show", description = "Show a course material", mixinStandardHelpOptions = true)
public class MaterialShowCommand implements Runnable {
    private final MaterialCommand parent;
    @Option(names = "--course", required = true, description = "Course code") private String courseCode;
    @Option(names = "--id", required = true, description = "Material id") private Long materialId;
    public MaterialShowCommand(MaterialCommand parent) { this.parent = parent; }
    @Override public void run() {
        Long courseId = parent.facade().getCourseByCode(courseCode).id();
        var material = parent.facade().getMaterial(courseId, materialId);
        System.out.printf("%d - %s%nType: %s%nStatus: %s%nCharacters: %d%nChunks: %d%nSHA-256: %s%n",
                material.id(), material.originalFilename(), material.fileType(), material.processingStatus(),
                material.extractedCharacterCount(), material.chunkCount(), material.sha256Hash());
    }
}
