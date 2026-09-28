package ca.yorku.eecs3311.coursepilot.application.search;

import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;

import java.util.List;

public record ScoredChunk(DocumentChunk chunk, double score, List<String> matchedTerms) {
    public ScoredChunk {
        matchedTerms = List.copyOf(matchedTerms);
    }
}
