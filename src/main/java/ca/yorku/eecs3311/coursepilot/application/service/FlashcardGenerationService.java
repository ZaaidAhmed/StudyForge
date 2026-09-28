package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.*;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.Normalizer;
import java.util.*;

@Service
public class FlashcardGenerationService {
    private static final Logger log = LoggerFactory.getLogger(FlashcardGenerationService.class);
    private final CourseMaterialService materialService; private final LlmClient llmClient;
    private final PromptBuilder promptBuilder; private final StructuredResponseParser parser;
    private final GroundingContextBuilder contextBuilder; private final FlashcardRepository repository;
    private final int maxContextCharacters; private final int defaultCount; private final int maxCount;

    public FlashcardGenerationService(CourseMaterialService materialService, LlmClient llmClient, PromptBuilder promptBuilder,
                                      StructuredResponseParser parser, GroundingContextBuilder contextBuilder, FlashcardRepository repository,
                                      @Value("${coursepilot.flashcards.max-context-characters:12000}") int maxContextCharacters,
                                      @Value("${coursepilot.flashcards.default-count:10}") int defaultCount,
                                      @Value("${coursepilot.flashcards.max-count:20}") int maxCount) {
        this.materialService = materialService; this.llmClient = llmClient; this.promptBuilder = promptBuilder; this.parser = parser;
        this.contextBuilder = contextBuilder; this.repository = repository; this.maxContextCharacters = maxContextCharacters;
        this.defaultCount = defaultCount; this.maxCount = maxCount;
    }

    @Transactional
    public FlashcardGenerationResult generate(Long courseId, Long documentId, Integer requestedCount) {
        int count = requestedCount == null ? defaultCount : requestedCount;
        if (count < 1 || count > maxCount) throw new IllegalArgumentException("Flashcard count must be between 1 and " + maxCount + ".");
        DocumentResponse document = materialService.getDocument(courseId, documentId);
        if (!"READY".equals(document.processingStatus())) throw new InsightGenerationException("This material is not ready for flashcard generation.");
        List<DocumentChunk> chunks = materialService.listChunks(courseId, documentId);
        List<SearchResult> results = chunks.stream().map(this::toSearchResult).toList();
        GroundingContextBuilder.GroundingContext context = contextBuilder.build(results, maxContextCharacters);
        if (context.sources().isEmpty()) throw new InsightGenerationException("No usable material was found for flashcard generation.");
        if (!llmClient.checkAvailability().isAvailable()) throw new InsightGenerationException("AI flashcard generation is currently unavailable. Existing flashcards can still be reviewed.");
        FlashcardGenerationLlmResponse generated;
        try { generated = parseGeneration(count, document.originalFilename(), context.text()); }
        catch (RuntimeException exception) {
            log.warn("Flashcard structured response parsing failed; retrying with compact context: {}", exception.toString());
            try { generated = parseGeneration(count, document.originalFilename(), contextBuilder.build(results, Math.min(maxContextCharacters, 4000)).text()); }
            catch (RuntimeException retryException) { throw new InsightGenerationException("The AI flashcards could not be processed safely.", retryException); }
        }
        Map<String, GroundedSource> sourceMap = new LinkedHashMap<>(); context.sources().forEach(source -> sourceMap.put(source.sourceId(), source));
        Set<String> questions = new LinkedHashSet<>(); List<Flashcard> saved = new ArrayList<>();
        for (FlashcardGenerationLlmResponse.GeneratedFlashcardDto dto : generated.flashcards()) {
            if (saved.size() >= count || dto.question() == null || dto.answer() == null) continue;
            String question = dto.question().trim(), answer = dto.answer().trim();
            if (question.isBlank() || answer.isBlank() || question.length() > 1000 || answer.length() > 3000) continue;
            if (!questions.add(normalize(question))) continue;
            List<GroundedSource> sources = validSources(dto.sourceIds(), sourceMap);
            if (sources.isEmpty()) { questions.remove(normalize(question)); continue; }
            Flashcard card = repository.save(new Flashcard(chunks.get(0).getDocument().getCourse(), question, answer));
            for (GroundedSource source : sources) {
                DocumentChunk chunk = chunks.stream().filter(c -> c.getId().equals(source.chunkId())).findFirst().orElse(null);
                if (chunk != null) card.addSource(new FlashcardSource(card, chunk.getDocument(), chunk.getId(), chunk.getChunkIndex(), chunk.getStartPage(), chunk.getEndPage()));
            }
            saved.add(card);
        }
        if (saved.isEmpty()) throw new InsightGenerationException("No valid flashcards were returned by the AI.");
        return new FlashcardGenerationResult(count, saved.size(), saved.stream().map(FlashcardResponse::from).toList());
    }

    private List<GroundedSource> validSources(List<String> ids, Map<String, GroundedSource> sourceMap) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> !sourceMap.containsKey(id))) return List.of();
        return ids.stream().map(sourceMap::get).distinct().toList();
    }
    private String normalize(String value) { return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    private FlashcardGenerationLlmResponse parseGeneration(int count, String label, String context) { return parser.parse(llmClient.generate(promptBuilder.buildFlashcardGenerationPrompt(count, label, context)).content(), FlashcardGenerationLlmResponse.class); }
    private SearchResult toSearchResult(DocumentChunk chunk) { return new SearchResult(chunk.getDocument().getId(), chunk.getId(), chunk.getDocument().getOriginalFilename(), chunk.getChunkIndex(), chunk.getContent(), 1d, chunk.getStartPage(), chunk.getEndPage(), List.of()); }
}
