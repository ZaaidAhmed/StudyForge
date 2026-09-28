package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record SearchResult(Long documentId, Long chunkId, String documentName, int chunkIndex,
                           String snippet, double score, Integer pageStart, Integer pageEnd,
                           List<String> matchedTerms) {
    public SearchResult {
        matchedTerms = List.copyOf(matchedTerms);
    }
}
