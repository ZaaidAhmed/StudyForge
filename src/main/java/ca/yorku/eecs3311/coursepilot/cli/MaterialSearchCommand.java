package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "search", description = "Search course materials", mixinStandardHelpOptions = true)
public class MaterialSearchCommand implements Runnable {
    private final MaterialCommand parent;
    @Option(names = "--course", required = true, description = "Course code") private String courseCode;
    @Option(names = "--query", required = true, description = "Search query") private String query;
    @Option(names = {"--limit", "--top-k"}, description = "Maximum results (default: 5)") private Integer limit;

    public MaterialSearchCommand(MaterialCommand parent) { this.parent = parent; }

    @Override
    public void run() {
        var course = parent.facade().getCourseByCode(courseCode);
        var results = parent.facade().searchMaterials(course.id(), query, limit);
        System.out.printf("Search results for \"%s\" in %s%n", query, course.courseCode());
        if (results.isEmpty()) {
            System.out.println("No relevant material was found in this course.");
            return;
        }
        for (int index = 0; index < results.size(); index++) print(index + 1, results.get(index));
    }

    private void print(int number, SearchResult result) {
        String pages = result.pageStart() == null ? "Chunk: " + result.chunkIndex()
                : result.pageStart().equals(result.pageEnd()) ? "Page: " + result.pageStart()
                : "Pages: " + result.pageStart() + "-" + result.pageEnd();
        System.out.printf("%d. %s%n   %s%n   %s%n", number, result.documentName(), pages, result.snippet());
    }
}
