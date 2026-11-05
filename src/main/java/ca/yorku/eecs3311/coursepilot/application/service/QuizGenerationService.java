package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.*;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

/** Generates grounded quizzes with observable, bounded recovery at the provider boundary. */
@Service
public class QuizGenerationService {
    private static final Logger log = LoggerFactory.getLogger(QuizGenerationService.class);
    private static final int MAX_PROVIDER_CALLS = 20;
    private final CourseMaterialService materials;
    private final LlmClient llm;
    private final PromptBuilder prompts;
    private final StructuredResponseParser parser;
    private final GroundingContextBuilder builder;
    private final QuizRepository quizzes;
    private final int maxContext;
    private final int generationContext;
    private final int maxCount;

    public QuizGenerationService(CourseMaterialService materials, LlmClient llm, PromptBuilder prompts,
                                 StructuredResponseParser parser, GroundingContextBuilder builder,
                                 QuizRepository quizzes,
                                 @Value("${coursepilot.quiz.max-context-characters:12000}") int maxContext,
                                 @Value("${coursepilot.quiz.generation-context-characters:6000}") int generationContext,
                                 @Value("${coursepilot.quiz.max-count:20}") int maxCount) {
        this.materials = materials; this.llm = llm; this.prompts = prompts; this.parser = parser;
        this.builder = builder; this.quizzes = quizzes; this.maxContext = maxContext;
        this.generationContext = Math.max(2000, Math.min(generationContext, maxContext)); this.maxCount = maxCount;
    }

    @Transactional
    public QuizGenerationResult generate(Long courseId, Long documentId, Integer count, Set<String> requestedTypes) {
        return generate(courseId, documentId, count, requestedTypes, CreationOrigin.USER);
    }

    @Transactional
    public QuizGenerationResult generate(Long courseId, Long documentId, Integer count, Set<String> requestedTypes,
                                         CreationOrigin origin) {
        int requestedCount = count == null ? 10 : count;
        if (requestedCount < 1 || requestedCount > maxCount) throw new IllegalArgumentException("Quiz count must be between 1 and " + maxCount + ".");
        Set<String> types = normalizeTypes(requestedTypes);
        DocumentResponse doc = materials.getDocument(courseId, documentId);
        if (!"READY".equals(doc.processingStatus())) throw new InsightGenerationException("This material is not ready for quiz generation.");
        List<DocumentChunk> chunks = materials.listChunks(courseId, documentId);
        if (chunks == null || chunks.isEmpty()) throw new InsightGenerationException("No usable material was found for quiz generation.");
        List<SearchResult> results = chunks.stream().map(c -> new SearchResult(c.getDocument().getId(), c.getId(), c.getDocument().getOriginalFilename(), c.getChunkIndex(), c.getContent(), 1d, c.getStartPage(), c.getEndPage(), List.of())).toList();
        GroundingContextBuilder.GroundingContext grounding = builder.build(results, maxContext);
        Map<String, GroundedSource> sourceMap = new LinkedHashMap<>(); grounding.sources().forEach(s -> sourceMap.put(s.sourceId(), s));
        List<QuizConcept> concepts = extractAssessmentConcepts(doc.originalFilename(), grounding.text(), sourceMap);
        if (concepts.isEmpty()) throw new InsightGenerationException("No teachable concepts were found for this quiz. Try another ready document.");
        LlmAvailability availability = llm.checkAvailability();
        if (!availability.isAvailable()) throw new InsightGenerationException("AI quiz generation is currently unavailable. Check AI status or switch providers.");

        String conceptContext = formatConcepts(concepts);
        List<String> allowedSourceIds = concepts.stream().flatMap(c -> c.sourceIds().stream()).distinct().toList();
        Quiz quiz = new Quiz(chunks.getFirst().getDocument().getCourse(), doc.originalFilename() + " quiz");
        quiz.setCreationOrigin(origin);
        List<QuizQuestion> knownQuestions = quizzes.findByCourseIdAndSourceDocumentId(courseId, documentId)
                .stream().flatMap(savedQuiz -> savedQuiz.getQuestions().stream()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Set<String> seen = knownQuestions.stream().map(question -> normalize(question.getPrompt()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        ObjectMapper mapper = new ObjectMapper();
        Stats stats = new Stats(requestedCount, concepts.size(), availability.provider(), availability.model()); int recoveryStage = 0; String failureHint = "the first generation request";

        while (quiz.getQuestions().size() < requestedCount && stats.providerCallCount < MAX_PROVIDER_CALLS && stats.generatedCandidateCount < requestedCount * 8) {
            int remaining = requestedCount - quiz.getQuestions().size(); int batchSize = Math.min(3, remaining); stats.attemptCount++;
            ProviderBatch batch = requestBatch(batchSize, types, doc.originalFilename(), conceptContext, allowedSourceIds, seen, recoveryStage, failureHint, stats);
            if (batch.responseCategory != null) { failureHint = batch.responseCategory; recoveryStage = Math.min(4, recoveryStage + 1); continue; }
            recoveryStage = 0; int before = quiz.getQuestions().size();
            for (QuizGenerationLlmResponse.GeneratedQuestion candidate : batch.questions) {
                if (quiz.getQuestions().size() >= requestedCount) break;
                String key = candidate == null || candidate.prompt() == null ? "" : normalize(candidate.prompt());
                if (key.isBlank() || seen.contains(key) || duplicatesExistingObjective(candidate, knownQuestions)) {
                    reject(stats, "DUPLICATE_QUESTION", candidate, "question repeats a learning objective in an existing or current quiz");
                    continue;
                }
                ValidationResult validation = toQuestion(candidate, quiz, chunks, sourceMap, types, mapper);
                if (validation.question() == null) { reject(stats, validation.reason(), candidate, validation.detail()); continue; }
                quiz.addQuestion(validation.question()); knownQuestions.add(validation.question()); seen.add(key); stats.validatedCount++;
            }
            if (quiz.getQuestions().size() == before) { failureHint = "the previous candidates were rejected; create replacement questions"; recoveryStage = Math.min(4, recoveryStage + 1); }
        }

        if (quiz.getQuestions().size() < requestedCount) {
            int before = quiz.getQuestions().size(); addFallbackQuestions(quiz, concepts, chunks, sourceMap, types, seen, knownQuestions, requestedCount, mapper, stats); stats.backfillAttempts++;
            log.info("Quiz fallback applied: before={}, after={}, requested={}", before, quiz.getQuestions().size(), requestedCount);
        }
        int saved = quiz.getQuestions().size(); stats.savedCount = saved; stats.returnedCount = saved;
        if (saved < requestedCount) {
            String category = stats.failureCategory.isBlank() ? "INSUFFICIENT_CONCEPTS" : stats.failureCategory;
            stats.failureCategory = category; String summary = stats.summary();
            log.warn("Quiz generation incomplete: {}", summary);
            String recovery = switch (category) {
                case "AI_INVALID_JSON" -> "The model returned an invalid quiz format. Try again or switch providers.";
                case "AI_INVALID_SCHEMA" -> "The model returned an unsupported quiz structure. Try again.";
                case "AI_EMPTY_RESPONSE", "AI_EMPTY_QUESTION_BATCH" -> "The model did not create questions from these concepts. Try again or choose another document.";
                case "AI_PROVIDER_ERROR", "AI_TIMEOUT" -> "AI is unavailable. Check AI status or switch providers.";
                default -> "Only " + saved + " distinct quiz-worthy questions could be created from " + concepts.size() + " validated concepts.";
            };
            throw new InsightGenerationException("The AI generated " + saved + " new, distinct questions out of " + requestedCount + ". "
                    + recovery + " Existing saved quizzes for this material were checked to prevent duplicate questions. " + summary + ".");
        }
        quizzes.save(quiz); log.info("Quiz generation complete: {}", stats.summary());
        return new QuizGenerationResult(requestedCount, saved, QuizResponseDto.from(quiz), stats.generatedCandidateCount, stats.validatedCount, stats.rejectedCount, stats.attemptCount, stats.providerCallCount, stats.responseCount, stats.parsedBatchCount, stats.retryCount, stats.failureCategory, stats.rejectionReasons, "Created " + saved + " of " + requestedCount + " questions.");
    }

    private ProviderBatch requestBatch(int count, Set<String> types, String label, String concepts, Collection<String> sourceIds, Collection<String> seen, int recoveryStage, String failureHint, Stats stats) {
        LlmRequest request = recoveryStage == 0 ? prompts.buildQuizGenerationPromptFromConcepts(count, String.join(",", types), label, concepts, sourceIds, seen) : prompts.buildQuizRecoveryPrompt(count, String.join(",", types), label, concepts, sourceIds, seen, failureHint);
        stats.providerCallCount++; LlmResponse response;
        try { response = llm.generate(request); }
        catch (LlmClientException exception) { stats.retryCount++; stats.failureCategory = exception.category().name(); log.warn("Quiz provider call failed: provider={}, model={}, attempt={}, category={}, exception={}", stats.provider, stats.model, stats.attemptCount, exception.category(), exception.getClass().getSimpleName()); return ProviderBatch.failed(exception.category().name()); }
        catch (RuntimeException exception) { stats.retryCount++; stats.failureCategory = "AI_PROVIDER_ERROR"; log.warn("Quiz provider call failed: provider={}, model={}, attempt={}, category=AI_PROVIDER_ERROR, exception={}", stats.provider, stats.model, stats.attemptCount, exception.getClass().getSimpleName()); return ProviderBatch.failed("AI_PROVIDER_ERROR"); }
        stats.responseCount++; String raw = response == null ? "" : Objects.toString(response.content(), "");
        boolean hasQuestions = raw.contains("\"questions\""); boolean hasQuizQuestions = raw.contains("\"quiz_questions\""); boolean hasItems = raw.contains("\"items\"");
        try {
            if (raw.isBlank()) throw new StructuredOutputParseException("empty response", null);
            QuizGenerationLlmResponse parsed = parser.parse(raw, QuizGenerationLlmResponse.class);
            if (parsed == null || parsed.questions() == null || parsed.questions().isEmpty()) {
                String category = hasQuestions || hasQuizQuestions || hasItems ? "AI_EMPTY_QUESTION_BATCH" : "AI_INVALID_SCHEMA"; stats.failureCategory = category; stats.retryCount++;
                safeResponseLog(stats, response, raw, hasQuestions, hasQuizQuestions, hasItems, false, 0, category); return ProviderBatch.failed(category);
            }
            stats.parsedBatchCount++; stats.generatedCandidateCount += parsed.questions().size(); safeResponseLog(stats, response, raw, hasQuestions, hasQuizQuestions, hasItems, true, parsed.questions().size(), "");
            return new ProviderBatch(parsed.questions(), null);
        } catch (StructuredOutputParseException exception) { stats.failureCategory = "AI_INVALID_JSON"; stats.retryCount++; safeResponseLog(stats, response, raw, hasQuestions, hasQuizQuestions, hasItems, false, 0, "AI_INVALID_JSON"); return ProviderBatch.failed("AI_INVALID_JSON"); }
        catch (RuntimeException exception) { stats.failureCategory = "AI_INVALID_SCHEMA"; stats.retryCount++; safeResponseLog(stats, response, raw, hasQuestions, hasQuizQuestions, hasItems, false, 0, "AI_INVALID_SCHEMA"); return ProviderBatch.failed("AI_INVALID_SCHEMA"); }
    }

    private void safeResponseLog(Stats stats, LlmResponse response, String raw, boolean hasQuestions, boolean hasQuizQuestions, boolean hasItems, boolean parsed, int candidateCount, String category) {
        log.info("Quiz response: provider={}, model={}, attempt={}, responseLength={}, startsWithJson={}, hasQuestionsField={}, hasQuizQuestionsField={}, hasItemsField={}, parseSuccess={}, candidateCount={}, failureCategory={}", stats.provider, response == null ? stats.model : response.model(), stats.attemptCount, raw.length(), raw.trim().startsWith("{"), hasQuestions, hasQuizQuestions, hasItems, parsed, candidateCount, category);
    }

    private void addFallbackQuestions(Quiz quiz, List<QuizConcept> concepts, List<DocumentChunk> chunks, Map<String, GroundedSource> sourceMap, Set<String> types, Set<String> seen, List<QuizQuestion> knownQuestions, int target, ObjectMapper mapper, Stats stats) {
        for (QuizConcept concept : concepts) {
            if (quiz.getQuestions().size() >= target) return;
            List<QuizQuestionType> angles = new ArrayList<>(); if (types.contains("MULTIPLE_CHOICE")) angles.add(QuizQuestionType.MULTIPLE_CHOICE); if (types.contains("TRUE_FALSE")) angles.add(QuizQuestionType.TRUE_FALSE); if (types.contains("SHORT_ANSWER")) angles.add(QuizQuestionType.SHORT_ANSWER);
            for (QuizQuestionType type : angles) {
                if (quiz.getQuestions().size() >= target) return; String prompt = fallbackPrompt(concept, type); String key = normalize(prompt);
                var candidate = fallbackCandidate(concept, type, concepts);
                if (seen.contains(key) || duplicatesExistingObjective(candidate, knownQuestions)) continue;
                ValidationResult validation = toQuestion(candidate, quiz, chunks, sourceMap, types, mapper);
                if (validation.question() == null) { reject(stats, validation.reason(), candidate, validation.detail()); continue; }
                quiz.addQuestion(validation.question()); knownQuestions.add(validation.question()); seen.add(key); stats.validatedCount++; stats.generatedCandidateCount++;
            }
        }
    }

    private QuizGenerationLlmResponse.GeneratedQuestion fallbackCandidate(QuizConcept concept, QuizQuestionType type, List<QuizConcept> all) {
        String prompt = fallbackPrompt(concept, type);
        if (type == QuizQuestionType.MULTIPLE_CHOICE) {
            List<String> options = new ArrayList<>(); options.add(concept.definition()); all.stream().filter(other -> !other.name().equalsIgnoreCase(concept.name())).map(QuizConcept::definition).filter(value -> !value.isBlank()).forEach(options::add); while (options.size() < 4) options.add("A different concept with a different primary purpose.");
            return new QuizGenerationLlmResponse.GeneratedQuestion(type.name(), prompt, options.subList(0, 4), options.getFirst(), concept.definition(), conciseExplanation("The correct option describes " + concept.name() + " because " + concept.definition()), concept.sourceIds());
        }
        if (type == QuizQuestionType.TRUE_FALSE) return new QuizGenerationLlmResponse.GeneratedQuestion(type.name(), prompt, List.of(), "true", concept.definition(), conciseExplanation(concept.definition()), concept.sourceIds());
        return new QuizGenerationLlmResponse.GeneratedQuestion(type.name(), prompt, List.of(), null, concept.definition(), conciseExplanation(concept.definition()), concept.sourceIds());
    }

    private String conciseExplanation(String value) {
        String text = Objects.toString(value, "").replaceAll("\\s+", " ").trim();
        if (text.length() <= 200) return text;
        int cut = text.lastIndexOf(' ', 200);
        return text.substring(0, cut > 80 ? cut : 200).trim();
    }

    private String fallbackPrompt(QuizConcept concept, QuizQuestionType type) { return switch (type) { case MULTIPLE_CHOICE -> "What is the primary idea of " + concept.name() + "?"; case TRUE_FALSE -> concept.name() + " is defined as: " + concept.definition(); case SHORT_ANSWER -> "How would you explain " + concept.name() + " to a classmate?"; }; }

    private Set<String> normalizeTypes(Set<String> requestedTypes) {
        Set<String> types = requestedTypes == null || requestedTypes.isEmpty() ? Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER") : requestedTypes.stream().map(this::normalizeType).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String type : types) try { QuizQuestionType.valueOf(type); } catch (Exception exception) { throw new IllegalArgumentException("Unsupported quiz question type: " + type); }
        return types;
    }

    private List<QuizConcept> extractAssessmentConcepts(String label, String context, Map<String, GroundedSource> sourceMap) {
        ConceptExtractionLlmResponse generated;
        try {
            LlmResponse first = llm.generate(prompts.buildConceptExtractionPrompt(label, context));
            generated = parser.parse(first == null ? "" : first.content(), ConceptExtractionLlmResponse.class);
            if (generated.concepts().isEmpty()) {
                LlmResponse retry = llm.generate(prompts.buildConceptExtractionRetryPrompt(label, context));
                generated = parser.parse(retry == null ? "" : retry.content(), ConceptExtractionLlmResponse.class);
            }
        } catch (RuntimeException exception) {
            log.warn("Quiz concept extraction failed: category={}, exception={}",
                    conceptFailureCategory(exception), exception.getClass().getSimpleName());
            generated = deterministicConceptResponse(sourceMap.values());
        }
        Map<String, QuizConcept> unique = new LinkedHashMap<>();
        for (ConceptExtractionLlmResponse.ConceptLlmResponse concept : generated.concepts()) {
            if (concept == null || concept.name() == null || concept.name().isBlank() || concept.definition() == null || concept.definition().isBlank()) continue;
            List<String> ids = concept.sourceIds() == null ? List.of() : concept.sourceIds().stream().filter(Objects::nonNull).map(value -> value.trim().toUpperCase(Locale.ROOT)).filter(sourceMap::containsKey).distinct().toList(); if (ids.isEmpty()) continue;
            unique.putIfAbsent(normalize(concept.name()), new QuizConcept(concept.name().trim(), concept.definition().trim(), concept.example() == null ? "" : concept.example().trim(), ids));
        }
        return new ArrayList<>(unique.values());
    }

    private ConceptExtractionLlmResponse deterministicConceptResponse(Collection<GroundedSource> sources) {
        List<ConceptExtractionLlmResponse.ConceptLlmResponse> concepts = new ArrayList<>();
        for (GroundedSource source : sources) {
            String snippet = source.snippet() == null ? "" : source.snippet().replaceAll("\\s+", " ").trim();
            if (snippet.length() < 35) continue;
            String name = snippet.split("[.!?:;]", 2)[0].trim();
            if (name.length() < 8) name = "Concept from source " + source.sourceId();
            if (name.length() > 90) name = name.substring(0, 89).trim();
            String definition = snippet.length() > 320 ? snippet.substring(0, 319).trim() + "…" : snippet;
            concepts.add(new ConceptExtractionLlmResponse.ConceptLlmResponse(
                    name, definition, "", List.of(source.sourceId())));
            if (concepts.size() >= 8) break;
        }
        log.info("Quiz concept fallback: extractedConcepts={}", concepts.size());
        return new ConceptExtractionLlmResponse(concepts);
    }

    private String conceptFailureCategory(RuntimeException exception) {
        if (exception instanceof StructuredOutputParseException) return "AI_INVALID_JSON";
        if (exception instanceof LlmClientException clientException) return clientException.category().name();
        return "AI_INVALID_SCHEMA";
    }

    private String formatConcepts(List<QuizConcept> concepts) { StringBuilder formatted = new StringBuilder(); for (QuizConcept concept : concepts) { formatted.append("CONCEPT: ").append(concept.name()).append('\n').append("DEFINITION: ").append(concept.definition()).append('\n'); if (!concept.example().isBlank()) formatted.append("EXAMPLE: ").append(concept.example()).append('\n'); formatted.append("SOURCE IDS: ").append(String.join(", ", concept.sourceIds())).append("\n\n"); } return formatted.toString().trim(); }

    private ValidationResult toQuestion(QuizGenerationLlmResponse.GeneratedQuestion q, Quiz quiz, List<DocumentChunk> chunks, Map<String, GroundedSource> sourceMap, Set<String> allowedTypes, ObjectMapper mapper) {
        if (q == null) return ValidationResult.rejected("MALFORMED", "question object is missing"); if (!isSelfContainedAssessmentQuestion(q.prompt())) return ValidationResult.rejected("INVALID_QUESTION", "question is not self-contained");
        QuizQuestionType type; try { type = QuizQuestionType.valueOf(normalizeType(q.type())); } catch (Exception exception) { return ValidationResult.rejected("INVALID_QUESTION_TYPE", "unsupported question type"); }
        if (!allowedTypes.contains(type.name())) return ValidationResult.rejected("INVALID_QUESTION_TYPE", "type was not requested"); if (q.sourceIds() == null || q.sourceIds().isEmpty() || q.sourceIds().stream().anyMatch(id -> id == null || !sourceMap.containsKey(id.trim().toUpperCase(Locale.ROOT)))) return ValidationResult.rejected("INVALID_SOURCE", "missing or invalid source reference");
        List<String> options = q.options() == null ? List.of() : q.options().stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList(); String correctAnswer = normalizeAnswer(q.correctAnswer(), options);
        if (type == QuizQuestionType.MULTIPLE_CHOICE && (options.size() != 4 || correctAnswer == null || !options.contains(correctAnswer) || options.stream().anyMatch(this::isForbiddenOption))) return ValidationResult.rejected("INVALID_OPTIONS", "MCQ must have exactly four plausible options and one correct answer");
        if (type == QuizQuestionType.TRUE_FALSE && !("true".equalsIgnoreCase(q.correctAnswer()) || "false".equalsIgnoreCase(q.correctAnswer()))) return ValidationResult.rejected("INVALID_ANSWER", "true/false answer is missing or invalid"); if (type == QuizQuestionType.SHORT_ANSWER && (q.referenceAnswer() == null || q.referenceAnswer().isBlank())) return ValidationResult.rejected("MALFORMED", "reference answer is missing");
        try { QuizQuestion question = new QuizQuestion(quiz, type, q.prompt().trim(), mapper.writeValueAsString(options), correctAnswer, q.referenceAnswer()); String explanation = q.explanation(); if (explanation == null || explanation.isBlank()) explanation = q.referenceAnswer(); if (explanation == null || explanation.isBlank()) explanation = "The correct answer is " + Objects.toString(correctAnswer, "the selected answer") + " because it matches the concept being tested."; question.setExplanation(conciseExplanation(explanation)); for (String id : q.sourceIds()) { GroundedSource source = sourceMap.get(id.trim().toUpperCase(Locale.ROOT)); DocumentChunk chunk = chunks.stream().filter(item -> item.getId().equals(source.chunkId())).findFirst().orElse(null); if (chunk != null) question.addSource(new QuizSource(question, chunk.getDocument(), chunk.getId(), chunk.getChunkIndex(), chunk.getStartPage(), chunk.getEndPage())); } if (question.getSources().isEmpty()) return ValidationResult.rejected("INVALID_SOURCE", "source does not belong to selected document"); return ValidationResult.accepted(question); }
        catch (Exception exception) { return ValidationResult.rejected("MALFORMED", "question could not be serialized"); }
    }

    private void reject(Stats stats, String reason, QuizGenerationLlmResponse.GeneratedQuestion question, String detail) { stats.rejectedCount++; stats.rejectionReasons.merge(reason == null || reason.isBlank() ? "MALFORMED" : reason, 1, Integer::sum); String prompt = question == null ? "<missing>" : Objects.toString(question.prompt(), "<missing>"); log.info("Quiz candidate rejected: attempt={}, reason={}, detail={}, promptLength={}", stats.attemptCount, reason, detail, prompt.length()); }
    private String normalizeAnswer(String answer, List<String> options) { if (answer == null) return null; String value = answer.trim(); if (value.length() == 1 && value.charAt(0) >= 'A' && value.charAt(0) <= 'Z') { int index = value.charAt(0) - 'A'; if (index < options.size()) return options.get(index); } return value; }
    private String normalize(String value) { return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    private boolean duplicatesExistingObjective(QuizGenerationLlmResponse.GeneratedQuestion candidate, List<QuizQuestion> known) {
        if (candidate == null || candidate.prompt() == null) return false;
        String prompt = normalize(candidate.prompt());
        String answer = normalize(Objects.toString(candidate.correctAnswer(), candidate.referenceAnswer()));
        Set<String> objective = objectiveTokens(prompt);
        for (QuizQuestion existing : known) {
            String existingPrompt = normalize(existing.getPrompt());
            if (prompt.equals(existingPrompt)) return true;
            Set<String> existingObjective = objectiveTokens(existingPrompt);
            if (!objective.isEmpty() && !existingObjective.isEmpty() && overlapCoefficient(objective, existingObjective) >= 0.80) return true;
            String existingAnswer = normalize(Objects.toString(existing.getCorrectAnswer(), existing.getReferenceAnswer()));
            if (answer.length() >= 24 && answer.equals(existingAnswer)) return true;
        }
        return false;
    }

    private Set<String> objectiveTokens(String prompt) {
        Set<String> ignored = Set.of("what", "which", "who", "when", "where", "why", "how", "does", "do", "is", "are", "was", "were", "the", "a", "an", "of", "in", "on", "for", "to", "from", "with", "by", "about", "explain", "define", "describe", "identify", "state", "main", "primary", "key", "following", "best", "correct", "term", "mean", "meant", "refer", "refers", "called", "known");
        Set<String> tokens = Arrays.stream(prompt.split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2 && !ignored.contains(token))
                .map(this::singularize)
                .collect(java.util.stream.Collectors.toSet());
        String value = " " + prompt + " ";
        if (value.matches("(?s).*\\b(what is|define|definition|meaning|refers to)\\b.*")) tokens.add("angle_definition");
        else if (value.matches("(?s).*\\b(compare|differ|difference|versus|vs)\\b.*")) tokens.add("angle_comparison");
        else if (value.matches("(?s).*\\b(why|benefit|advantage|tradeoff|purpose|importance)\\b.*")) tokens.add("angle_reasoning");
        else if (value.matches("(?s).*\\b(scenario|example|apply|application|use|used|when)\\b.*")) tokens.add("angle_application");
        return tokens;
    }

    private String singularize(String token) {
        if (token.endsWith("ies") && token.length() > 4) return token.substring(0, token.length() - 3) + "y";
        if (token.endsWith("es") && token.length() > 5) return token.substring(0, token.length() - 2);
        if (token.endsWith("s") && token.length() > 4 && !token.endsWith("ss")) return token.substring(0, token.length() - 1);
        return token;
    }

    private double overlapCoefficient(Set<String> first, Set<String> second) {
        Set<String> smaller = first.size() <= second.size() ? first : second;
        Set<String> larger = smaller == first ? second : first;
        long intersection = smaller.stream().filter(larger::contains).count();
        return (double) intersection / smaller.size();
    }
    private String normalizeType(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); }
    private boolean isSelfContainedAssessmentQuestion(String prompt) { if (prompt == null) return false; String normalized = normalize(prompt); if (normalized.length() < 20 || normalized.length() > 500) return false; return List.of("uploaded material", "this passage", "the passage", "provided pdf", "the material states", "supported by the material", "what does the material", "according to the document", "according to this document").stream().noneMatch(normalized::contains); }
    private boolean isForbiddenOption(String option) { String normalized = normalize(option); return normalized.contains("all of the above") || normalized.contains("none of the above") || normalized.contains("uploaded material") || normalized.contains("provided pdf"); }

    private record QuizConcept(String name, String definition, String example, List<String> sourceIds) { }
    private record ValidationResult(QuizQuestion question, String reason, String detail) { private static ValidationResult accepted(QuizQuestion question) { return new ValidationResult(question, "", ""); } private static ValidationResult rejected(String reason, String detail) { return new ValidationResult(null, reason, detail); } }
    private record ProviderBatch(List<QuizGenerationLlmResponse.GeneratedQuestion> questions, String responseCategory) { private static ProviderBatch failed(String category) { return new ProviderBatch(List.of(), category); } }
    private static final class Stats {
        final int requestedCount; final int conceptCount; final String provider; final String model; int attemptCount, providerCallCount, responseCount, parsedBatchCount, generatedCandidateCount, validatedCount, savedCount, returnedCount, rejectedCount, retryCount, backfillAttempts; String failureCategory = ""; final Map<String, Integer> rejectionReasons = new LinkedHashMap<>();
        Stats(int requestedCount, int conceptCount, String provider, String model) { this.requestedCount = requestedCount; this.conceptCount = conceptCount; this.provider = provider; this.model = model; }
        String summary() { return "requested=" + requestedCount + ", concepts=" + conceptCount + ", attempts=" + attemptCount + ", providerCalls=" + providerCallCount + ", responses=" + responseCount + ", parsedBatches=" + parsedBatchCount + ", generatedCandidates=" + generatedCandidateCount + ", validated=" + validatedCount + ", saved=" + savedCount + ", returned=" + returnedCount + ", rejected=" + rejectedCount + ", retries=" + retryCount + ", backfillAttempts=" + backfillAttempts + ", failureCategory=" + failureCategory + ", reasons=" + rejectionReasons; }
    }
}
