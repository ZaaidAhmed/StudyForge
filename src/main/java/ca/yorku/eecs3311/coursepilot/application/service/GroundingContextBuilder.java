package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GroundingContextBuilder {
    public GroundingContext build(List<SearchResult> results, int maxCharacters) {
        List<GroundedSource> sources = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        int used = 0;
        for (SearchResult result : results) {
            String sourceId = "S" + (sources.size() + 1);
            GroundedSource source = new GroundedSource(sourceId, result.documentId(), result.documentName(),
                    result.chunkId(), result.chunkIndex(), result.pageStart(), result.pageEnd(), result.snippet());
            String section = format(source);
            if (!sections.isEmpty() && used + section.length() > maxCharacters) break;
            if (sections.isEmpty() && section.length() > maxCharacters) {
                String shortened = result.snippet().substring(0, Math.min(result.snippet().length(), Math.max(1, maxCharacters / 2)));
                source = new GroundedSource(sourceId, result.documentId(), result.documentName(), result.chunkId(),
                        result.chunkIndex(), result.pageStart(), result.pageEnd(), shortened);
                section = format(source);
            }
            sources.add(source);
            sections.add(section);
            used += section.length();
        }
        return new GroundingContext(List.copyOf(sources), String.join("\n\n", sections));
    }

    private String format(GroundedSource source) {
        String location = source.pageStart() == null ? "Chunk: " + source.chunkIndex()
                : source.pageStart().equals(source.pageEnd()) ? "Page: " + source.pageStart()
                : "Pages: " + source.pageStart() + "-" + source.pageEnd();
        return "SOURCE " + source.sourceId() + "\nDocument: " + source.documentName() + "\n" + location
                + "\nContent:\n" + source.snippet();
    }

    public record GroundingContext(List<GroundedSource> sources, String text) { }
}
