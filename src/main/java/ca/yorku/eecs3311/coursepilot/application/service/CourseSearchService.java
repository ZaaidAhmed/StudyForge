package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.application.search.NormalizedQuery;
import ca.yorku.eecs3311.coursepilot.application.search.QueryNormalizer;
import ca.yorku.eecs3311.coursepilot.application.search.RetrievalScorer;
import ca.yorku.eecs3311.coursepilot.application.search.ScoredChunk;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import ca.yorku.eecs3311.coursepilot.domain.model.ProcessingStatus;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseSearchService {
    private final CourseRepository courseRepository;
    private final DocumentChunkRepository chunkRepository;
    private final QueryNormalizer queryNormalizer;
    private final RetrievalScorer scorer;
    private final int defaultTopK;
    private final int maxTopK;
    private final double minimumScore;

    public CourseSearchService(CourseRepository courseRepository, DocumentChunkRepository chunkRepository,
                               QueryNormalizer queryNormalizer, RetrievalScorer scorer,
                               @Value("${coursepilot.retrieval.default-top-k:5}") int defaultTopK,
                               @Value("${coursepilot.retrieval.max-top-k:20}") int maxTopK,
                               @Value("${coursepilot.retrieval.minimum-score:1.0}") double minimumScore) {
        this.courseRepository = courseRepository;
        this.chunkRepository = chunkRepository;
        this.queryNormalizer = queryNormalizer;
        this.scorer = scorer;
        this.defaultTopK = defaultTopK;
        this.maxTopK = maxTopK;
        this.minimumScore = minimumScore;
    }

    public List<SearchResult> search(Long courseId, String query, Integer requestedTopK) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException("Course with id '" + courseId + "' was not found.");
        }
        NormalizedQuery normalized = queryNormalizer.normalize(query);
        if (normalized.terms().isEmpty()) throw new IllegalArgumentException("Search query is required.");
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        if (topK < 1 || topK > maxTopK) {
            throw new IllegalArgumentException("Search result limit must be between 1 and " + maxTopK + ".");
        }
        return rank(chunkRepository.findSearchableByCourseId(courseId, ProcessingStatus.READY), normalized, topK);
    }

    public List<SearchResult> searchDocument(Long courseId, Long documentId, String query, Integer requestedTopK) {
        if (!courseRepository.existsById(courseId)) throw new CourseNotFoundException("Course with id '" + courseId + "' was not found.");
        NormalizedQuery normalized = queryNormalizer.normalize(query);
        if (normalized.terms().isEmpty()) throw new IllegalArgumentException("Search query is required.");
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        if (topK < 1 || topK > maxTopK) throw new IllegalArgumentException("Search result limit must be between 1 and " + maxTopK + ".");
        List<DocumentChunk> chunks = chunkRepository.findSearchableByCourseIdAndDocumentId(courseId, documentId, ProcessingStatus.READY);
        if (!chunks.isEmpty()) return rank(chunks, normalized, topK);
        throw new DocumentNotFoundException("Material with id '" + documentId + "' was not found or is not ready for this course.");
    }

    private List<SearchResult> rank(List<DocumentChunk> chunks, NormalizedQuery normalized, int topK) {
        return chunks.stream()
                .map(chunk -> scorer.score(normalized, chunk))
                .filter(scored -> scored.score() >= minimumScore)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()
                        .thenComparingLong(this::documentId)
                        .thenComparingInt(scored -> scored.chunk().getChunkIndex()))
                .limit(topK)
                .map(this::toResult)
                .toList();
    }

    private SearchResult toResult(ScoredChunk scored) {
        DocumentChunk chunk = scored.chunk();
        return new SearchResult(chunk.getDocument().getId(), chunk.getId(), chunk.getDocument().getOriginalFilename(),
                chunk.getChunkIndex(), snippet(chunk.getContent(), scored.matchedTerms()), scored.score(),
                chunk.getStartPage(), chunk.getEndPage(), scored.matchedTerms());
    }

    private String snippet(String content, List<String> matchedTerms) {
        if (content.length() <= 360) return content;
        int start = matchedTerms.stream().mapToInt(term -> content.toLowerCase(java.util.Locale.ROOT).indexOf(term)).filter(value -> value >= 0).min().orElse(0);
        int from = Math.max(0, start - 100);
        int to = Math.min(content.length(), from + 360);
        String result = content.substring(from, to).trim();
        return (from > 0 ? "…" : "") + result + (to < content.length() ? "…" : "");
    }

    private long documentId(ScoredChunk scored) {
        return scored.chunk().getDocument().getId() == null ? Long.MAX_VALUE : scored.chunk().getDocument().getId();
    }
}
