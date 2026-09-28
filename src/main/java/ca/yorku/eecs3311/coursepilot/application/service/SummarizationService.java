package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.PromptBuilder;
import ca.yorku.eecs3311.coursepilot.application.ai.StructuredResponseParser;
import ca.yorku.eecs3311.coursepilot.application.ai.SummaryLlmResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.application.dto.SummaryResult;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SummarizationService {
    public static final String NO_EVIDENCE = "No sufficiently relevant course material was found for this topic.";
    private final CourseMaterialService materialService;
    private final CourseSearchService searchService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final StructuredResponseParser parser;
    private final GroundingContextBuilder contextBuilder;
    private final int maxContextCharacters;
    private final int retrievalTopK;

    public SummarizationService(CourseMaterialService materialService, CourseSearchService searchService,
                                LlmClient llmClient, PromptBuilder promptBuilder,
                                StructuredResponseParser parser, GroundingContextBuilder contextBuilder,
                                @Value("${coursepilot.summary.max-context-characters:12000}") int maxContextCharacters,
                                @Value("${coursepilot.summary.retrieval-top-k:8}") int retrievalTopK) {
        this.materialService = materialService;
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.contextBuilder = contextBuilder;
        this.maxContextCharacters = maxContextCharacters;
        this.retrievalTopK = retrievalTopK;
    }

    public SummaryResult summarizeDocument(Long courseId, Long documentId) {
        DocumentResponse document = materialService.getDocument(courseId, documentId);
        requireReady(document);
        List<SearchResult> chunks = materialService.listChunks(courseId, documentId).stream()
                .map(this::toSearchResult).toList();
        return generate("document", document.originalFilename(), chunks,
                "AI summarization is currently unavailable.");
    }

    public SummaryResult summarizeTopic(Long courseId, String topic) {
        String normalized = topic == null ? "" : topic.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("Topic is required.");
        if (normalized.length() > 200) throw new IllegalArgumentException("Topic must be at most 200 characters.");
        List<SearchResult> results = searchService.search(courseId, normalized, retrievalTopK);
        if (results.isEmpty()) return new SummaryResult("topic", normalized, NO_EVIDENCE, List.of(), List.of(), true, true);
        return generate("topic", normalized, results, "AI topic summarization is currently unavailable.");
    }

    private SummaryResult generate(String scope, String label, List<SearchResult> results, String unavailableMessage) {
        if (results.isEmpty()) return new SummaryResult(scope, label, "No usable material was found in this scope.", List.of(), List.of(), true, true);
        GroundingContextBuilder.GroundingContext context = contextBuilder.build(results, maxContextCharacters);
        if (context.sources().isEmpty()) return new SummaryResult(scope, label, "No usable material was found in this scope.", List.of(), List.of(), true, true);
        if (!llmClient.checkAvailability().isAvailable()) throw new InsightGenerationException(unavailableMessage);
        LlmRequest request = promptBuilder.buildSummaryPrompt(scope, label, context.text());
        SummaryLlmResponse generated;
        try {
            generated = parser.parse(llmClient.generate(request).content(), SummaryLlmResponse.class);
        } catch (RuntimeException exception) {
            throw new InsightGenerationException("The AI summary could not be processed safely.", exception);
        }
        if (generated.insufficientContext()) return new SummaryResult(scope, label, NO_EVIDENCE, List.of(), List.of(), true, true);
        if (generated.summary() == null || generated.summary().isBlank())
            throw new InsightGenerationException("The AI summary was empty.");
        List<GroundedSource> sources = validSources(generated.sourceIds(), context.sources());
        if (sources.isEmpty()) throw new InsightGenerationException("The AI summary did not contain valid supporting course sources.");
        return new SummaryResult(scope, label, generated.summary().trim(), generated.keyPoints(), sources, true, false);
    }

    private List<GroundedSource> validSources(List<String> ids, List<GroundedSource> supplied) {
        Map<String, GroundedSource> sourceMap = supplied.stream().collect(Collectors.toUnmodifiableMap(GroundedSource::sourceId, Function.identity()));
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> !sourceMap.containsKey(id)))
            throw new InsightGenerationException("The AI response did not contain valid supporting course sources.");
        return ids.stream().map(sourceMap::get).distinct().toList();
    }

    private void requireReady(DocumentResponse document) {
        if (!"READY".equals(document.processingStatus()))
            throw new InsightGenerationException("This material is not ready for AI study tools. Wait for processing to finish or upload another file.");
    }

    private SearchResult toSearchResult(DocumentChunk chunk) {
        return new SearchResult(chunk.getDocument().getId(), chunk.getId(), chunk.getDocument().getOriginalFilename(),
                chunk.getChunkIndex(), chunk.getContent(), 1.0d, chunk.getStartPage(), chunk.getEndPage(), List.of());
    }
}
