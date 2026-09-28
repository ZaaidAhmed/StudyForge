package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.ConceptExtractionLlmResponse;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.PromptBuilder;
import ca.yorku.eecs3311.coursepilot.application.ai.StructuredResponseParser;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ExtractedConcept;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ConceptExtractionService {
    private final CourseMaterialService materialService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final StructuredResponseParser parser;
    private final GroundingContextBuilder contextBuilder;
    private final int maxContextCharacters;

    public ConceptExtractionService(CourseMaterialService materialService, LlmClient llmClient,
                                   PromptBuilder promptBuilder, StructuredResponseParser parser,
                                   GroundingContextBuilder contextBuilder,
                                   @Value("${coursepilot.concepts.max-context-characters:12000}") int maxContextCharacters) {
        this.materialService = materialService;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.contextBuilder = contextBuilder;
        this.maxContextCharacters = maxContextCharacters;
    }

    public List<ExtractedConcept> extract(Long courseId, Long documentId) {
        DocumentResponse document = materialService.getDocument(courseId, documentId);
        if (!"READY".equals(document.processingStatus()))
            throw new InsightGenerationException("This material is not ready for AI study tools. Wait for processing to finish or upload another file.");
        List<SearchResult> chunks = materialService.listChunks(courseId, documentId).stream().map(this::toSearchResult).toList();
        GroundingContextBuilder.GroundingContext context = contextBuilder.build(chunks, maxContextCharacters);
        if (context.sources().isEmpty()) return List.of();
        if (!llmClient.checkAvailability().isAvailable()) throw new InsightGenerationException("AI concept extraction is currently unavailable.");
        ConceptExtractionLlmResponse generated;
        try {
            generated = parser.parse(llmClient.generate(promptBuilder.buildConceptExtractionPrompt(document.originalFilename(), context.text())).content(), ConceptExtractionLlmResponse.class);
        } catch (RuntimeException exception) {
            throw new InsightGenerationException("The AI concepts could not be processed safely.", exception);
        }
        Map<String, GroundedSource> sourceMap = new LinkedHashMap<>();
        context.sources().forEach(source -> sourceMap.put(source.sourceId(), source));
        Map<String, MutableConcept> merged = new LinkedHashMap<>();
        for (ConceptExtractionLlmResponse.ConceptLlmResponse concept : generated.concepts()) {
            if (concept.name() == null || concept.name().isBlank() || concept.definition() == null || concept.definition().isBlank())
                throw new InsightGenerationException("The AI concepts contained an incomplete concept.");
            List<GroundedSource> sources = validateSources(concept.sourceIds(), sourceMap);
            String key = normalizeKey(concept.name());
            MutableConcept existing = merged.get(key);
            if (existing == null) merged.put(key, new MutableConcept(concept.name().trim(), concept.definition().trim(), sources));
            else existing.sources.addAll(sources);
        }
        return merged.values().stream().map(value -> new ExtractedConcept(value.name, value.definition, List.copyOf(value.sources))).toList();
    }

    private List<GroundedSource> validateSources(List<String> ids, Map<String, GroundedSource> sourceMap) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> !sourceMap.containsKey(id)))
            throw new InsightGenerationException("The AI concepts contained an invalid source reference.");
        LinkedHashSet<GroundedSource> sources = new LinkedHashSet<>();
        ids.forEach(id -> sources.add(sourceMap.get(id)));
        return new ArrayList<>(sources);
    }

    private String normalizeKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private SearchResult toSearchResult(DocumentChunk chunk) {
        return new SearchResult(chunk.getDocument().getId(), chunk.getId(), chunk.getDocument().getOriginalFilename(),
                chunk.getChunkIndex(), chunk.getContent(), 1.0d, chunk.getStartPage(), chunk.getEndPage(), List.of());
    }

    private static final class MutableConcept {
        private final String name;
        private final String definition;
        private final LinkedHashSet<GroundedSource> sources = new LinkedHashSet<>();
        private MutableConcept(String name, String definition, List<GroundedSource> sources) {
            this.name = name; this.definition = definition; this.sources.addAll(sources);
        }
    }
}
