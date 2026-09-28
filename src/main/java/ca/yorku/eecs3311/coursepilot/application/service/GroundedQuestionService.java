package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.GroundedAnswerLlmResponse;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.PromptBuilder;
import ca.yorku.eecs3311.coursepilot.application.ai.StructuredResponseParser;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedAnswer;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroundedQuestionService {
    public static final String INSUFFICIENT_MESSAGE = "I couldn't find enough relevant information in the uploaded course materials to answer this question.";
    private final CourseSearchService searchService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final StructuredResponseParser responseParser;
    private final GroundingContextBuilder contextBuilder;
    private final int retrievalTopK;
    private final int maxQuestionLength;
    private final int maxContextCharacters;

    public GroundedQuestionService(CourseSearchService searchService, LlmClient llmClient, PromptBuilder promptBuilder,
                                   StructuredResponseParser responseParser, GroundingContextBuilder contextBuilder,
                                   @Value("${coursepilot.qa.retrieval-top-k:5}") int retrievalTopK,
                                   @Value("${coursepilot.qa.max-question-length:1000}") int maxQuestionLength,
                                   @Value("${coursepilot.qa.max-context-characters:12000}") int maxContextCharacters) {
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.contextBuilder = contextBuilder;
        this.retrievalTopK = retrievalTopK;
        this.maxQuestionLength = maxQuestionLength;
        this.maxContextCharacters = maxContextCharacters;
    }

    public GroundedAnswer ask(Long courseId, String question) {
        String normalizedQuestion = validateQuestion(question);
        return answerFromResults(normalizedQuestion, searchService.search(courseId, normalizedQuestion, retrievalTopK));
    }

    public GroundedAnswer askDocument(Long courseId, Long documentId, String question) {
        String normalizedQuestion = validateQuestion(question);
        return answerFromResults(normalizedQuestion, searchService.searchDocument(courseId, documentId, normalizedQuestion, retrievalTopK));
    }

    private GroundedAnswer answerFromResults(String normalizedQuestion, List<SearchResult> results) {
        if (results.isEmpty()) return GroundedAnswer.insufficient(normalizedQuestion, INSUFFICIENT_MESSAGE);
        GroundingContextBuilder.GroundingContext context = contextBuilder.build(results, maxContextCharacters);
        LlmRequest request = promptBuilder.buildGroundedQuestionPrompt(normalizedQuestion, context.text());
        LlmAvailability availability = llmClient.checkAvailability();
        if (!availability.isAvailable()) throw new GroundedQuestionException("AI answering is currently unavailable. You can still use material search.");
        GroundedAnswerLlmResponse generated;
        try {
            generated = responseParser.parse(llmClient.generate(request).content(), GroundedAnswerLlmResponse.class);
        } catch (RuntimeException exception) {
            throw new GroundedQuestionException("The AI response could not be processed safely.", exception);
        }
        if (generated.insufficientContext()) return GroundedAnswer.insufficient(normalizedQuestion, INSUFFICIENT_MESSAGE);
        Map<String, GroundedSource> sourceMap = context.sources().stream()
                .collect(Collectors.toUnmodifiableMap(GroundedSource::sourceId, Function.identity()));
        if (generated.answer().isBlank() || generated.sourceIds().isEmpty()
                || generated.sourceIds().stream().anyMatch(id -> !sourceMap.containsKey(id))) {
            throw new GroundedQuestionException("The AI response did not contain valid supporting course sources.");
        }
        List<GroundedSource> sources = generated.sourceIds().stream().map(sourceMap::get).distinct().toList();
        return new GroundedAnswer(normalizedQuestion, generated.answer(), sources, true, false);
    }

    private String validateQuestion(String question) {
        String normalized = question == null ? "" : question.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("Question is required.");
        if (normalized.length() > maxQuestionLength) throw new IllegalArgumentException("Question must be at most " + maxQuestionLength + " characters.");
        return normalized;
    }
}
