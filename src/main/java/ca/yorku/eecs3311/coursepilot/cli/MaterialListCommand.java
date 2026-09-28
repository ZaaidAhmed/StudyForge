package ca.yorku.eecs3311.coursepilot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List course materials", mixinStandardHelpOptions = true)
public class MaterialListCommand implements Runnable {
    private final MaterialCommand parent;
    @Option(names = "--course", required = true, description = "Course code") private String courseCode;
    public MaterialListCommand(MaterialCommand parent) { this.parent = parent; }
    @Override public void run() {
        var materials = parent.facade().listMaterials(parent.facade().getCourseByCode(courseCode).id());
        if (materials.isEmpty()) { System.out.println("No course materials found."); return; }
        materials.forEach(material -> System.out.printf("%d  %s  %s  %d chunks%n", material.id(), material.originalFilename(), material.processingStatus(), material.chunkCount()));
    }
}
