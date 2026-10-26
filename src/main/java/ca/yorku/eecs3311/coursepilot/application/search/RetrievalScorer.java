package ca.yorku.eecs3311.coursepilot.application.search;

import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;

public interface RetrievalScorer {
    ScoredChunk score(NormalizedQuery query, DocumentChunk chunk);
}
