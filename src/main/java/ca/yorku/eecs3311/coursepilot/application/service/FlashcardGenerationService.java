package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.ConceptExtractionLlmResponse;
import ca.yorku.eecs3311.coursepilot.application.ai.AiFailureCategory;
import ca.yorku.eecs3311.coursepilot.application.ai.FlashcardGenerationLlmResponse;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClientException;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.PromptBuilder;
import ca.yorku.eecs3311.coursepilot.application.ai.StructuredResponseParser;
import ca.yorku.eecs3311.coursepilot.application.ai.StructuredOutputParseException;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardGenerationResult;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardSource;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FlashcardGenerationService {
    private static final Logger log = LoggerFactory.getLogger(FlashcardGenerationService.class);
    private static final Set<String> CARD_TYPES = Set.of("definition", "comparison", "advantage", "example", "concept");

    private final CourseMaterialService materialService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final StructuredResponseParser parser;
    private final GroundingContextBuilder contextBuilder;
    private final FlashcardRepository repository;
    private final FlashcardMaterialPreprocessor preprocessor = new FlashcardMaterialPreprocessor();
    private final int maxContextCharacters;
    private final int defaultCount;
    private final int maxCount;

    public FlashcardGenerationService(CourseMaterialService materialService, LlmClient llmClient,
                                      PromptBuilder promptBuilder, StructuredResponseParser parser,
                                      GroundingContextBuilder contextBuilder, FlashcardRepository repository,
                                      @Value("${coursepilot.flashcards.max-context-characters:12000}") int maxContextCharacters,
                                      @Value("${coursepilot.flashcards.default-count:10}") int defaultCount,
                                      @Value("${coursepilot.flashcards.max-count:20}") int maxCount) {
        this.materialService = materialService; this.llmClient = llmClient; this.promptBuilder = promptBuilder;
        this.parser = parser; this.contextBuilder = contextBuilder; this.repository = repository;
        this.maxContextCharacters = maxContextCharacters; this.defaultCount = defaultCount; this.maxCount = maxCount;
    }

    @Transactional
    public FlashcardGenerationResult generate(Long courseId, Long documentId, Integer requestedCount) {
        return generate(courseId, documentId, requestedCount,
                ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.USER);
    }

    @Transactional
    public FlashcardGenerationResult generate(Long courseId, Long documentId, Integer requestedCount,
                                               ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin origin) {
        int count = requestedCount == null ? defaultCount : requestedCount;
        if (count < 1 || count > maxCount) throw new IllegalArgumentException("Flashcard count must be between 1 and " + maxCount + ".");
        var document = materialService.getDocument(courseId, documentId);
        if (!"READY".equals(document.processingStatus())) throw new InsightGenerationException("This material is not ready for flashcard generation.");
        List<DocumentChunk> chunks = materialService.listChunks(courseId, documentId);
        if (chunks == null || chunks.isEmpty()) throw new InsightGenerationException("No usable material was found for flashcard generation.");
        if (!llmClient.checkAvailability().isAvailable()) throw new InsightGenerationException("AI flashcard generation is currently unavailable. Existing flashcards can still be reviewed.");

        List<SearchResult> cleaned = preprocessor.clean(chunks);
        GroundingContextBuilder.GroundingContext context = contextBuilder.build(cleaned, maxContextCharacters);
        if (context.sources().isEmpty()) throw new InsightGenerationException("No clean, usable study material was found for flashcard generation.");
        Map<String, GroundedSource> sourceMap = sourceMap(context.sources());
        List<Concept> concepts = validateConcepts(extractConcepts(document.originalFilename(), context.text()), sourceMap);
        if (concepts.isEmpty()) throw new InsightGenerationException("No teachable concepts were found in this document. Try another ready document.");
        List<LearningAngle> learningAngles = buildLearningAngles(concepts);

        List<Flashcard> saved = new ArrayList<>();
        List<CardSignature> sessionKnown = new ArrayList<>();
        sessionKnown.addAll(repository
                .findByCourseIdAndSourceDocumentIdWithSources(courseId, documentId)
                .stream()
                .map(card -> signature(card.getQuestion(), card.getAnswer(), null, null))
                .toList());
        int existingCount = sessionKnown.size();
        Set<String> consumedAngles = new LinkedHashSet<>();
        List<String> excludedCards = new ArrayList<>();
        GenerationStats stats = new GenerationStats();
        stats.availableAngles = learningAngles.size();
        int maxAttempts = 12;
        log.info("Flashcard trace: requestedCount={}, existingCount={}, extractedConcepts={}, availableAngles={}, sourceChunks={}",
                count, existingCount, concepts.size(), learningAngles.size(), chunks.size());
        for (int attempt = 0; attempt < maxAttempts && saved.size() < count; attempt++) {
            int remaining = count - saved.size();
            FlashcardGenerationLlmResponse generated;
            try {
                generated = attempt == 0
                        ? generateCards(remaining, document.originalFilename(), formatConcepts(concepts), excludedCards, stats)
                        : generateReplacementCards(remaining, document.originalFilename(), formatConcepts(concepts),
                        formatUnusedAngles(learningAngles, consumedAngles), excludedCards, stats);
            } catch (InsightGenerationException exception) {
                if (saved.isEmpty()) throw exception;
                log.warn("Flashcard trace: generation attempt {} failed after {} cards were saved: {}", attempt + 1, saved.size(), exception.getMessage());
                break;
            }
            log.info("Flashcard trace: attempt={}, requestedBatch={}, aiGeneratedBatch={}", attempt + 1, remaining, generated.flashcards().size());
            int before = saved.size();
            List<Flashcard> batch = persistValidCards(remaining, generated, sourceMap, chunks, sessionKnown, consumedAngles, stats, origin);
            saved.addAll(batch);
            batch.forEach(card -> sessionKnown.add(signature(card.getQuestion(), card.getAnswer(), null, null)));
            generated.flashcards().forEach(card -> {
                if (card != null && card.question() != null && card.answer() != null) {
                    excludedCards.add(cleanText(card.question()) + " — " + cleanText(card.answer()));
                }
            });
            log.info("Flashcard trace: attempt={}, validatedTotal={}, deduplicatedTotal={}, rejectedTotal={}, savedTotal={}",
                    attempt + 1, stats.validated, stats.deduplicated, stats.rejected, saved.size());
            if (saved.size() == before && consumedAngles.size() >= learningAngles.size()) break;
        }
        if (saved.size() < count && !concepts.isEmpty()) {
            List<Flashcard> fallback = createFallbackCards(count - saved.size(),
                    learningAngles, chunks, sessionKnown, consumedAngles, stats, origin);
            saved.addAll(fallback);
        }
        if (saved.isEmpty()) {
            String failure = stats.failureCategory.isBlank() ? "AI_EMPTY_FLASHCARD_BATCH" : stats.failureCategory;
            log.error("Flashcard generation produced no cards: requestedCount={}, conceptCount={}, generatedCount={}, rejectedCount={}, retryCount={}, category={}",
                    count, concepts.size(), stats.generated, stats.rejected, stats.retryCount, failure);
            throw new InsightGenerationException(userMessage(parseCategory(failure)) + " "
                    + "We extracted the document successfully, but no usable cards were saved. "
                    + "Document pages: " + document.pageCount() + "; concepts extracted: " + concepts.size()
                    + "; cards generated: " + stats.generated + "; cards rejected: " + stats.rejected + ".");
        }
        String reason = saved.size() == count ? "" : "We created " + saved.size() + " of " + count
                + " flashcards. All distinct concepts and learning angles in this document have been used.";
        repository.flush();
        long databaseCount = repository.countByCourseIdAndSourceDocumentId(courseId, documentId);
        long expectedDatabaseCount = (long) existingCount + saved.size();
        if (databaseCount < expectedDatabaseCount) {
            log.error("Flashcard persistence mismatch: existingCount={}, savedCount={}, databaseCount={}, expectedMinimum={}",
                    existingCount, saved.size(), databaseCount, expectedDatabaseCount);
            throw new InsightGenerationException("Flashcards were generated but could not be fully saved. Please try again.");
        }
        int returnedCount = saved.size();
        log.info("Flashcard trace final: requestedCount={}, existingCount={}, conceptCount={}, availableAngles={}, generatedCount={}, validatedCount={}, deduplicatedCount={}, rejectedCount={}, savedCount={}, databaseCount={}, returnedCount={}, backfillAttempts={}, rejectionReasons={}",
                count, existingCount, concepts.size(), stats.availableAngles, stats.generated, stats.validated,
                stats.deduplicated, stats.rejected, stats.saved, databaseCount, returnedCount,
                stats.backfillAttempts, stats.rejectionReasons);
        log.info("Flashcard generation complete: requested={}, generated={}, rejected={}, accepted={}, rejectionReasons={}",
                count, stats.generated, stats.rejected, saved.size(), stats.rejectionReasons);
        List<FlashcardResponse> returned = saved.stream().map(FlashcardResponse::from).toList();
        if (saved.size() != count) {
            stats.failureCategory = "INSUFFICIENT_UNIQUE_CONCEPTS";
            log.warn("Flashcard generation incomplete: requestedCount={}, savedCount={}, conceptCount={}, category={}",
                    count, saved.size(), concepts.size(), stats.failureCategory);
        }
        return new FlashcardGenerationResult(count, saved.size(), returned, reason,
                stats.generated, stats.validated, stats.deduplicated, stats.rejected, (int) databaseCount,
                returned.size(), stats.rejectionReasons, stats.retryCount, stats.failureCategory, stats.backfillAttempts);
    }

    private ConceptExtractionLlmResponse extractConcepts(String label, String context) {
        try {
            var response = parser.parse(llmClient.generate(promptBuilder.buildConceptExtractionPrompt(label, context)).content(), ConceptExtractionLlmResponse.class);
            if (!response.concepts().isEmpty()) return response;
            return parser.parse(llmClient.generate(promptBuilder.buildConceptExtractionRetryPrompt(label, context)).content(), ConceptExtractionLlmResponse.class);
        } catch (RuntimeException exception) {
            throw new InsightGenerationException("The document concepts could not be extracted safely.", exception);
        }
    }

    private FlashcardGenerationLlmResponse generateCards(int count, String label, String conceptContext,
                                                         List<String> excludedCards, GenerationStats stats) {
        var request = promptBuilder.buildFlashcardFromConceptsPrompt(count, label, conceptContext, excludedCards);
        for (int retry = 0; retry < 2; retry++) {
            if (retry > 0) stats.retryCount++;
            long started = System.nanoTime();
            try {
                var llmResponse = llmClient.generate(request);
                String content = llmResponse.content() == null ? "" : llmResponse.content().trim();
                log.info("Flashcard AI response: provider={}, model={}, requestedCount={}, retry={}, elapsedMs={}, responseLength={}, startsWithJson={}, containsFlashcardsField={}, containsFlashcardsArray={}",
                        llmClient.getClass().getSimpleName(), llmResponse.model(), count, retry, llmResponse.durationMs(), content.length(),
                        startsWithJson(content), content.contains("flashcards"), content.contains("\"flashcards\""));
                FlashcardGenerationLlmResponse parsed;
                try {
                    parsed = parser.parse(content, FlashcardGenerationLlmResponse.class);
                } catch (StructuredOutputParseException exception) {
                    stats.failureCategory = AiFailureCategory.AI_INVALID_JSON.name();
                    log.warn("Flashcard generation failed: provider={}, model={}, stage=response-parse, category={}, exception={}, responseLength={}, retry={}",
                            llmClient.getClass().getSimpleName(), llmResponse.model(), stats.failureCategory, exception.getClass().getSimpleName(), content.length(), retry + 1);
                    if (retry == 0) {
                        request = promptBuilder.buildFlashcardRecoveryPrompt(count, label, conceptContext, excludedCards,
                                "Return valid JSON; the previous response was not parseable.");
                        continue;
                    }
                    throw new InsightGenerationException("The model returned an invalid flashcard format. Try again or switch AI providers.", exception);
                }
                if (!content.contains("\"flashcards\"") && !content.contains("\"flashcards")) {
                    stats.failureCategory = AiFailureCategory.AI_INVALID_SCHEMA.name();
                    log.warn("Flashcard generation failed: provider={}, model={}, stage=response-schema, category={}, responseLength={}, retry={}",
                            llmClient.getClass().getSimpleName(), llmResponse.model(), stats.failureCategory, content.length(), retry + 1);
                    if (retry == 0) {
                        request = promptBuilder.buildFlashcardRecoveryPrompt(count, label, conceptContext, excludedCards,
                                "The previous response was missing the required flashcards array.");
                        continue;
                    }
                    throw new InsightGenerationException("The model returned the wrong flashcard structure. Try again or switch AI providers.");
                }
                if (parsed.flashcards().isEmpty()) {
                    stats.failureCategory = AiFailureCategory.AI_EMPTY_FLASHCARD_BATCH.name();
                    log.warn("Flashcard generation failed: provider={}, model={}, stage=response-content, category={}, responseLength={}, retry={}",
                            llmClient.getClass().getSimpleName(), llmResponse.model(), stats.failureCategory, content.length(), retry + 1);
                    if (retry == 0) {
                        request = promptBuilder.buildFlashcardRecoveryPrompt(count, label, conceptContext, excludedCards,
                                "The previous response contained an empty flashcards array. Create cards from the valid concepts.");
                        continue;
                    }
                }
                return parsed;
            } catch (LlmClientException exception) {
                stats.failureCategory = exception.category().name();
                log.warn("Flashcard generation failed: provider={}, model={}, stage=provider, category={}, exception={}, message={}, retry={}",
                        llmClient.getClass().getSimpleName(), "configured", stats.failureCategory,
                        exception.getClass().getSimpleName(), safeMessage(exception), retry + 1);
                if (retry == 0 && exception.category() != AiFailureCategory.AI_TIMEOUT) {
                    request = promptBuilder.buildFlashcardRecoveryPrompt(count, label, conceptContext, excludedCards,
                            "The previous provider response was unavailable. Return strict JSON only.");
                    continue;
                }
                throw new InsightGenerationException(userMessage(exception.category()), exception);
            } catch (InsightGenerationException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                stats.failureCategory = AiFailureCategory.AI_PROVIDER_ERROR.name();
                log.warn("Flashcard generation failed: stage=response-processing, category={}, exception={}, message={}, retry={}, elapsedMs={}",
                        stats.failureCategory, exception.getClass().getSimpleName(), safeMessage(exception), retry + 1,
                        (System.nanoTime() - started) / 1_000_000);
                if (retry == 0) {
                    request = promptBuilder.buildFlashcardRecoveryPrompt(count, label, conceptContext, excludedCards,
                            "The previous response could not be processed. Return strict JSON only.");
                    continue;
                }
                throw new InsightGenerationException("The AI flashcards could not be processed safely. Check AI status and try again.", exception);
            }
        }
        return new FlashcardGenerationLlmResponse(List.of());
    }

    private FlashcardGenerationLlmResponse generateReplacementCards(int count, String label, String conceptContext,
                                                                    String unusedAngles, List<String> excludedCards,
                                                                    GenerationStats stats) {
        stats.backfillAttempts++;
        var request = promptBuilder.buildFlashcardReplacementPrompt(count, label, conceptContext, unusedAngles, excludedCards);
        return generateCardsWithRequest(count, request, stats);
    }

    private FlashcardGenerationLlmResponse generateCardsWithRequest(int count, ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest request,
                                                                    GenerationStats stats) {
        for (int retry = 0; retry < 2; retry++) {
            if (retry > 0) stats.retryCount++;
            try {
                var response = llmClient.generate(request);
                String content = response.content() == null ? "" : response.content().trim();
                FlashcardGenerationLlmResponse parsed = parser.parse(content, FlashcardGenerationLlmResponse.class);
                if (!content.contains("\"flashcards\"") || parsed.flashcards().isEmpty()) {
                    stats.failureCategory = parsed.flashcards().isEmpty()
                            ? AiFailureCategory.AI_EMPTY_FLASHCARD_BATCH.name() : AiFailureCategory.AI_INVALID_SCHEMA.name();
                    if (retry == 0) continue;
                    return new FlashcardGenerationLlmResponse(List.of());
                }
                return parsed;
            } catch (LlmClientException exception) {
                stats.failureCategory = exception.category().name();
                if (retry == 0 && exception.category() != AiFailureCategory.AI_TIMEOUT) continue;
                throw new InsightGenerationException(userMessage(exception.category()), exception);
            } catch (StructuredOutputParseException exception) {
                stats.failureCategory = AiFailureCategory.AI_INVALID_JSON.name();
                if (retry == 0) continue;
                log.warn("Flashcard replacement response was not valid JSON: count={}, responseType={}", count, exception.getClass().getSimpleName());
                return new FlashcardGenerationLlmResponse(List.of());
            }
        }
        return new FlashcardGenerationLlmResponse(List.of());
    }

    private String userMessage(AiFailureCategory category) {
        return switch (category) {
            case AI_TIMEOUT -> "AI timed out while creating flashcards. Try again or switch providers.";
            case AI_BLOCKED_RESPONSE -> "The AI provider blocked this flashcard request. Try another document or provider.";
            case AI_EMPTY_RESPONSE, AI_EMPTY_FLASHCARD_BATCH -> "The model did not create flashcards from these concepts. Try again or choose another ready document.";
            case AI_INVALID_JSON -> "The model returned an invalid flashcard format. Try again or switch AI providers.";
            case AI_INVALID_SCHEMA -> "The model returned the wrong flashcard structure. Try again or switch AI providers.";
            default -> "AI flashcard generation is unavailable. Check AI status and try again.";
        };
    }

    private AiFailureCategory parseCategory(String value) {
        try { return AiFailureCategory.valueOf(value); }
        catch (IllegalArgumentException exception) { return AiFailureCategory.AI_PROVIDER_ERROR; }
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "(no message)";
        return message.replaceAll("(?i)(api[- ]?key|authorization|bearer)\\s*[:=]\\s*[^ ,;]+", "$1=[redacted]");
    }

    private boolean startsWithJson(String content) {
        return content.startsWith("{") || content.startsWith("[") || content.startsWith("```");
    }

    private List<Concept> validateConcepts(ConceptExtractionLlmResponse response, Map<String, GroundedSource> sourceMap) {
        Map<String, Concept> unique = new LinkedHashMap<>();
        for (var concept : response.concepts()) {
            if (concept == null || concept.name() == null || concept.definition() == null || concept.name().isBlank() || concept.definition().isBlank()) continue;
            List<GroundedSource> sources = validSources(concept.sourceIds(), sourceMap);
            if (sources.isEmpty()) continue;
            String name = cleanText(concept.name()), definition = cleanText(concept.definition()), example = cleanText(concept.example());
            if (name.length() < 3 || definition.length() < 20 || definition.length() > 700) continue;
            unique.putIfAbsent(normalize(name), new Concept(name, definition, example, sources));
        }
        return List.copyOf(unique.values());
    }

    private String formatConcepts(List<Concept> concepts) {
        StringBuilder out = new StringBuilder();
        int index = 1;
        for (Concept concept : concepts) {
            out.append("CONCEPT C").append(index++).append("\nName: ").append(concept.name)
                    .append("\nDefinition: ").append(concept.definition).append("\n");
            if (!concept.example.isBlank()) out.append("Example: ").append(concept.example).append("\n");
            out.append("Source IDs: ").append(concept.sources.stream().map(GroundedSource::sourceId).reduce((a, b) -> a + ", " + b).orElse(""))
                    .append("\n\n");
        }
        return out.toString().trim();
    }

    private List<Flashcard> persistValidCards(int count, FlashcardGenerationLlmResponse response,
                                               Map<String, GroundedSource> sourceMap, List<DocumentChunk> chunks,
                                               List<CardSignature> sessionKnown, Set<String> consumedAngles,
                                               GenerationStats stats,
                                               ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin origin) {
        List<CardSignature> knownCards = repository
                .findByCourseIdAndSourceDocumentIdWithSources(chunks.get(0).getDocument().getCourse().getId(),
                        chunks.get(0).getDocument().getId())
                .stream()
                .map(card -> signature(card.getQuestion(), card.getAnswer(), null, null))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        knownCards.addAll(sessionKnown);
        List<Flashcard> saved = new ArrayList<>();
        for (var dto : response.flashcards()) {
            if (saved.size() >= count) break;
            stats.generated++;
            if (dto == null || dto.question() == null || dto.answer() == null) {
                reject(stats, "invalidFormat", dto == null ? "<null>" : dto.question(), dto == null ? "" : dto.answer(), "missing question or answer");
                continue;
            }
            String question = cleanText(dto.question()), answer = cleanText(dto.answer());
            List<GroundedSource> sources = validSources(dto.sourceIds(), sourceMap);
            if (!CARD_TYPES.contains(normalize(dto.type()))) {
                reject(stats, "invalidFormat", question, answer, "unsupported card type");
                continue;
            }
            if (sources.isEmpty()) {
                reject(stats, "invalidFormat", question, answer, "missing or invalid source reference");
                continue;
            }
            QualityResult quality = qualityScore(question, answer, sources);
            if (quality.score() < 65) {
                reject(stats, quality.reason(), question, answer, "quality score=" + quality.score());
                continue;
            }
            stats.validated++;
            CardSignature candidate = signature(question, answer, dto.concept(), dto.learningObjective());
            if (knownCards.stream().anyMatch(existing -> isDuplicate(candidate, existing))) {
                reject(stats, "duplicate", question, answer, "learning objective already covered");
                stats.deduplicated++;
                continue;
            }
            Flashcard card = new Flashcard(chunks.get(0).getDocument().getCourse(), question, answer);
            card.setCreationOrigin(origin);
            repository.save(card);
            for (GroundedSource source : sources) {
                DocumentChunk chunk = chunks.stream().filter(value -> value.getId().equals(source.chunkId())).findFirst().orElse(null);
                if (chunk != null) card.addSource(new FlashcardSource(card, chunk.getDocument(), chunk.getId(), chunk.getChunkIndex(), chunk.getStartPage(), chunk.getEndPage()));
            }
            saved.add(card);
            stats.saved++;
            knownCards.add(candidate);
            consumedAngles.add(angleKey(dto.concept(), dto.learningObjective(), question));
        }
        return saved;
    }

    private List<Flashcard> createFallbackCards(int target, List<LearningAngle> angles, List<DocumentChunk> chunks,
                                                List<CardSignature> sessionKnown, Set<String> consumedAngles,
                                                GenerationStats stats,
                                                ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin origin) {
        List<Flashcard> fallback = new ArrayList<>();
        for (LearningAngle angle : angles) {
            if (fallback.size() >= target) break;
            if (consumedAngles.contains(angle.key())) continue;
            FallbackCard candidateText = fallbackCard(angle);
            stats.generated++;
            QualityResult quality = qualityScore(candidateText.question(), candidateText.answer(), angle.sources());
            CardSignature candidate = signature(candidateText.question(), candidateText.answer(), angle.conceptFingerprint(), angle.objective());
            boolean fallbackCriticalFailure = candidateText.question().isBlank() || candidateText.answer().isBlank()
                    || looksLikeOcrGarbage(candidateText.question()) || looksLikeOcrGarbage(candidateText.answer())
                    || isObviouslySourceReferenced(candidateText.question());
            if (fallbackCriticalFailure || sessionKnown.stream().anyMatch(existing -> isDuplicate(candidate, existing))) {
                reject(stats, fallbackCriticalFailure ? quality.reason() : "duplicate", candidateText.question(), candidateText.answer(), "fallback candidate");
                continue;
            }
            stats.validated++;
            Flashcard card = new Flashcard(chunks.get(0).getDocument().getCourse(), candidateText.question(), candidateText.answer());
            card.setCreationOrigin(origin);
            repository.save(card);
            for (GroundedSource source : angle.sources()) {
                    chunks.stream().filter(value -> value.getId().equals(source.chunkId())).findFirst().ifPresent(chunk ->
                            card.addSource(new FlashcardSource(card, chunk.getDocument(), chunk.getId(), chunk.getChunkIndex(), chunk.getStartPage(), chunk.getEndPage())));
            }
            sessionKnown.add(candidate);
            consumedAngles.add(angle.key());
            fallback.add(card);
            stats.saved++;
            log.info("Fallback flashcard accepted: concept={}, type={}, score={}", angle.conceptFingerprint(), candidateText.type(), quality.score());
        }
        return fallback;
    }

    private boolean conceptAlreadyCovered(Concept concept, List<CardSignature> knownCards) {
        Set<String> conceptTokens = meaningfulTokens(normalize(concept.name));
        if (conceptTokens.isEmpty()) return false;
        return knownCards.stream().anyMatch(card -> card.combinedTokens().containsAll(conceptTokens));
    }

    private List<LearningAngle> buildLearningAngles(List<Concept> concepts) {
        List<LearningAngle> angles = new ArrayList<>();
        for (Concept concept : concepts) {
            String fingerprint = normalize(concept.name);
            addAngle(angles, concept, fingerprint, "definition", "definition of " + fingerprint);
            addAngle(angles, concept, fingerprint, "advantage", "importance of " + fingerprint);
            addAngle(angles, concept, fingerprint, "example", "example of " + fingerprint);
            addAngle(angles, concept, fingerprint, "concept", "recognition of " + fingerprint);
            addAngle(angles, concept, fingerprint, "concept", "design implication of " + fingerprint);
            addAngle(angles, concept, fingerprint, "advantage", "tradeoff of " + fingerprint);
        }
        return angles;
    }

    private void addAngle(List<LearningAngle> angles, Concept concept, String fingerprint, String type, String objective) {
        angles.add(new LearningAngle(fingerprint, concept.name, concept.definition, concept.example,
                type, objective, concept.sources, fingerprint + "|" + objective));
    }

    private String formatUnusedAngles(List<LearningAngle> angles, Set<String> consumed) {
        StringBuilder text = new StringBuilder();
        int index = 1;
        for (LearningAngle angle : angles) {
            if (!consumed.contains(angle.key())) {
                text.append("A").append(index++).append(" concept=").append(angle.conceptName())
                        .append(" type=").append(angle.type()).append(" objective=").append(angle.objective()).append("\n");
            }
        }
        return text.toString().trim();
    }

    private String angleKey(String concept, String learningObjective, String question) {
        String conceptKey = normalize(concept);
        if (conceptKey.isBlank()) conceptKey = questionFingerprint(normalize(question));
        String objective = normalize(learningObjective);
        if (objective.isBlank()) objective = questionObjective(normalize(question), "");
        return conceptKey + "|" + objective;
    }

    private FallbackCard fallbackCard(LearningAngle angle) {
        return switch (angle.type()) {
            case "definition" -> new FallbackCard("What is " + angle.conceptName() + "?", angle.definition(), "definition");
            case "advantage" -> new FallbackCard("Why is " + angle.conceptName() + " important?",
                    "Its importance comes from this principle: " + angle.definition(), "advantage");
            case "example" -> new FallbackCard("What is an example of " + angle.conceptName() + " in practice?",
                    angle.example().isBlank() ? "A practical example applies this idea by following its defining principle: " + angle.definition() : angle.example(), "example");
            case "concept" -> new FallbackCard("How can you recognize the use of " + angle.conceptName() + "?",
                    "Look for a design or process that applies this idea: " + angle.definition(), "concept");
            default -> new FallbackCard("What practical effect does " + angle.conceptName() + " have?",
                    "It affects the design or process by applying this principle: " + angle.definition(), "concept");
        };
    }

    /**
     * Deduplicates at the learning-objective level. This deliberately stays
     * provider-neutral and deterministic: it does not require an embedding
     * service, network call, or another model invocation during persistence.
     */
    private boolean isDuplicate(CardSignature candidate, CardSignature existing) {
        if (!candidate.fingerprint().isBlank() && candidate.fingerprint().equals(existing.fingerprint())) return true;
        if (!candidate.objective().isBlank() && candidate.objective().equals(existing.objective())) return true;
        if (!candidate.answer().isBlank() && candidate.answer().equals(existing.answer())) return true;

        double questionSimilarity = jaccard(candidate.questionTokens(), existing.questionTokens());
        double answerSimilarity = jaccard(candidate.answerTokens(), existing.answerTokens());
        double combinedSimilarity = jaccard(candidate.combinedTokens(), existing.combinedTokens());
        return Math.max(questionSimilarity, Math.max(answerSimilarity, combinedSimilarity)) >= 0.80d;
    }

    private CardSignature signature(String question, String answer, String concept, String learningObjective) {
        String normalizedQuestion = normalize(question);
        String normalizedAnswer = normalize(answer);
        Set<String> questionTokens = meaningfulTokens(normalizedQuestion);
        Set<String> answerTokens = meaningfulTokens(normalizedAnswer);
        Set<String> combined = new LinkedHashSet<>(questionTokens);
        combined.addAll(answerTokens);
        String fingerprint = meaningfulTokens(normalize(concept)).stream().sorted().collect(java.util.stream.Collectors.joining(" "));
        if (fingerprint.isBlank()) fingerprint = questionFingerprint(normalizedQuestion);
        String objective = questionObjective(normalizedQuestion, learningObjective);
        return new CardSignature(normalizedQuestion, normalizedAnswer, fingerprint, objective,
                questionTokens, answerTokens, combined);
    }

    private String questionFingerprint(String question) {
        Set<String> tokens = meaningfulTokens(question);
        if (tokens.isEmpty()) return "";
        // Normalize common wording variants so “What is a thread?” and
        // “Define a thread in an operating system” share one fingerprint.
        Set<String> normalized = new LinkedHashSet<>();
        for (String token : tokens) {
            String canonical = switch (token) {
                case "define", "definition", "meaning", "mean" -> "";
                case "cheaper", "economical", "inexpensive", "costly", "cost", "expensive" -> "cost";
                case "differ", "difference", "versus", "compared" -> "comparison";
                case "benefit", "benefits", "advantage", "advantages" -> "benefit";
                case "creation", "creating", "created" -> "create";
                case "threads" -> "thread";
                case "processes" -> "process";
                default -> token;
            };
            if (!canonical.isBlank() && !Set.of("operating", "system", "systems", "question", "term").contains(canonical)) normalized.add(canonical);
        }
        return normalized.stream().sorted().collect(java.util.stream.Collectors.joining(" "));
    }

    private String questionObjective(String question, String suppliedObjective) {
        String objective = normalize(suppliedObjective);
        if (!objective.isBlank()) return questionFingerprint(objective);
        if (question.matches(".*\\b(what is|define|meaning|mean)\\b.*")) return "definition:" + questionFingerprint(question);
        if (question.matches(".*\\b(why|benefit|advantage|cheaper|economical|less expensive)\\b.*")) return "benefit:" + questionFingerprint(question);
        if (question.matches(".*\\b(differ|difference|versus|compare|compared)\\b.*")) return "comparison:" + questionFingerprint(question);
        return "";
    }

    private Set<String> meaningfulTokens(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) return 0d;
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "when", "where", "which", "who", "why", "how", "does", "do", "did", "is", "are", "was", "were",
            "the", "this", "that", "these", "those", "a", "an", "and", "or", "but", "to", "of", "for", "from", "in",
            "on", "with", "about", "into", "than", "then", "can", "could", "should", "would", "will", "its", "their",
            "your", "one", "same", "make", "makes", "made", "term", "thing", "using", "used", "use", "explain", "explanation");

    private record CardSignature(String question, String answer, String fingerprint, String objective,
                                 Set<String> questionTokens, Set<String> answerTokens, Set<String> combinedTokens) { }

    private QualityResult qualityScore(String question, String answer, List<GroundedSource> sources) {
        if (question.isBlank() || answer.isBlank()) return new QualityResult(0, "tooShort");
        if (looksLikeOcrGarbage(question) || looksLikeOcrGarbage(answer)) return new QualityResult(0, "ocrArtifact");
        if (question.length() < 10 || meaningfulTokens(question).size() < 2 || !question.contains(" ")
                || isObviouslySourceReferenced(question))
            return new QualityResult(40, "notSelfContained");
        if (containsSourceFiller(answer)) return new QualityResult(0, "sourceReferenced");
        if (mostlyCopied(answer, sources)) return new QualityResult(0, "copiedSource");
        int score = 70;
        if (question.length() >= 35 && question.length() <= 220) score += 10;
        if (answer.length() >= 20 && answer.length() <= 650) score += 8;
        long sentences = Arrays.stream(answer.split("(?<=[.!?])\\s+")).filter(part -> !part.isBlank()).count();
        if (sentences >= 1 && sentences <= 5) score += 7;
        return new QualityResult(Math.max(0, Math.min(100, score)), score < 65 ? "lowQuality" : "accepted");
    }

    private boolean looksLikeOcrGarbage(String value) {
        if (value.indexOf('\uFFFD') >= 0 || value.matches(".*[ÂÃâ][^a-zA-Z ]?.*")) return true;
        long letters = value.chars().filter(Character::isLetterOrDigit).count();
        long symbols = value.chars().filter(character -> !Character.isLetterOrDigit(character) && !Character.isWhitespace(character)).count();
        return letters < 5 || (letters > 0 && symbols > letters * 2);
    }

    private boolean isObviouslySourceReferenced(String question) {
        String normalized = normalize(question);
        return normalized.contains("this passage") || normalized.contains("the passage")
                || normalized.contains("the material") || normalized.contains("the document")
                || normalized.contains("what is being taught") || normalized.contains("what is this about");
    }

    private void reject(GenerationStats stats, String reason, String question, String answer, String detail) {
        stats.rejected++;
        stats.rejectionReasons.merge(reason, 1, Integer::sum);
        log.info("Flashcard rejected: Question=\"{}\", Answer=\"{}\", Rejected because: {} ({})",
                question == null ? "" : question, answer == null ? "" : answer, reason, detail);
    }

    private boolean mostlyCopied(String answer, List<GroundedSource> sources) {
        String normalizedAnswer = normalize(answer);
        Set<String> answerWords = new LinkedHashSet<>(Arrays.asList(normalizedAnswer.split("\\s+")));
        for (GroundedSource source : sources) {
            String sourceText = normalize(source.snippet());
            if (sourceText.contains(normalizedAnswer)) return true;
            long shared = answerWords.stream().filter(sourceText::contains).count();
            if (!answerWords.isEmpty() && ((double) shared / answerWords.size()) > .86d) return true;
        }
        return false;
    }

    private boolean containsSourceFiller(String answer) {
        String normalized = normalize(answer);
        return normalized.startsWith("according to the document") || normalized.startsWith("according to the passage")
                || normalized.startsWith("the passage says") || normalized.startsWith("this text discusses")
                || normalized.startsWith("this document discusses");
    }

    private List<GroundedSource> validSources(List<String> ids, Map<String, GroundedSource> sourceMap) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> !sourceMap.containsKey(id))) return List.of();
        return ids.stream().map(sourceMap::get).distinct().toList();
    }

    private Map<String, GroundedSource> sourceMap(List<GroundedSource> sources) {
        Map<String, GroundedSource> map = new LinkedHashMap<>();
        sources.forEach(source -> map.put(source.sourceId(), source));
        return map;
    }

    private String cleanText(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) { return cleanText(value).toLowerCase(Locale.ROOT); }

    private record Concept(String name, String definition, String example, List<GroundedSource> sources) { }
    private record LearningAngle(String conceptFingerprint, String conceptName, String definition, String example,
                                 String type, String objective, List<GroundedSource> sources, String key) { }
    private record FallbackCard(String question, String answer, String type) { }
    private record QualityResult(int score, String reason) { }
    private static final class GenerationStats {
        private int generated;
        private int validated;
        private int deduplicated;
        private int rejected;
        private int saved;
        private int retryCount;
        private int backfillAttempts;
        private int availableAngles;
        private String failureCategory = "";
        private final Map<String, Integer> rejectionReasons = new LinkedHashMap<>();
    }
}
