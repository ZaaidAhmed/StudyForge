package ca.yorku.eecs3311.coursepilot.application.agent;

import ca.yorku.eecs3311.coursepilot.application.dto.GroundedAnswer;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.SummaryResult;
import ca.yorku.eecs3311.coursepilot.application.service.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentToolManagerTest {
    private final ProgressService progress = mock(ProgressService.class);
    private final LearningHistoryService history = mock(LearningHistoryService.class);
    private final GroundedQuestionService questions = mock(GroundedQuestionService.class);
    private final CourseMaterialService materials = mock(CourseMaterialService.class);
    private final SummarizationService summaries = mock(SummarizationService.class);
    private final FlashcardGenerationService flashcards = mock(FlashcardGenerationService.class);
    private final QuizGenerationService quizzes = mock(QuizGenerationService.class);
    private final AgentToolManager tools = new AgentToolManager(progress, history,
            mock(RecommendationService.class), materials, mock(CourseSearchService.class),
            questions, flashcards, quizzes,
            summaries, mock(StudyPlanService.class));

    @Test
    void weakTopicResultsExcludeTheSeparateQuizPerformanceMetric() {
        when(progress.summarize(36L)).thenReturn(new ProgressSummary(36L, "Operating Systems", 11, 46, "WEAK",
                List.of(new ProgressSummary.TopicProgress("CPU Scheduling", 72, "DEVELOPING", 10),
                        new ProgressSummary.TopicProgress("Quiz performance", 20, "WEAK", 1))));

        var result = tools.execute(new AgentPlanStep("GET_WEAK_TOPICS", Map.of()), 36L);

        assertThat(result.output()).contains("CPU Scheduling", "72%", "developing")
                .doesNotContain("Quiz performance", "20%");
    }

    @Test
    void courseQuestionUsesGroundedAnswerAndReturnsCitationWithoutRawChunk() {
        String question = "What is a thread in an operating system?";
        when(questions.ask(36L, question)).thenReturn(new GroundedAnswer(question,
                "A thread is an execution unit within a process with its own program counter, registers, and stack.",
                List.of(new GroundedSource("S1", 72L, "Threads.pdf", 14L, 3, 7, 7, "internal retrieved text")),
                true, false));

        var result = tools.execute(new AgentPlanStep("ANSWER_COURSE_QUESTION", Map.of("question", question)), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("A thread is an execution unit", "Threads", "p. 7")
                .doesNotContain("internal retrieved text", "ANSWER_COURSE_QUESTION");
        verify(questions).ask(36L, question);
    }

    @Test
    void courseSummaryUsesAllReadyMaterialsInsteadOfSelectingTheFirstDocument() {
        when(summaries.summarizeCourse(36L)).thenReturn(new SummaryResult("course",
                "All ready course materials (2 documents)", "Combined course overview.",
                List.of("Threads", "CPU scheduling"), List.of(), true, false));

        var result = tools.execute(new AgentPlanStep("GENERATE_SUMMARY", Map.of()), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("All ready course materials (2 documents)", "Combined course overview.", "CPU scheduling");
        verify(summaries).summarizeCourse(36L);
        verify(materials, never()).listDocuments(36L);
    }

    @Test
    void explicitMaterialSummaryUsesOnlyTheSelectedDocument() {
        when(materials.listDocuments(36L)).thenReturn(List.of(new DocumentResponse(12L, "CPU Scheduling.pdf",
                "pdf", "application/pdf", "hash", "READY", java.time.Instant.now(), 100, 1, 1)));
        when(summaries.summarizeDocument(36L, 12L)).thenReturn(new SummaryResult("document", "CPU Scheduling.pdf",
                "Scheduling summary.", List.of(), List.of(), true, false));

        var result = tools.execute(new AgentPlanStep("GENERATE_SUMMARY",
                Map.of("request", "Summarize document: CPU Scheduling.pdf")), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("CPU Scheduling.pdf", "Scheduling summary.");
        verify(summaries).summarizeDocument(36L, 12L);
        verify(summaries, never()).summarizeCourse(36L);
    }

    @Test
    void getHistoryUsesSummarizedCourseActivitiesAndFormatsThemWithoutToolNames() {
        when(history.list(36L)).thenReturn(List.of(new ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent(
                java.time.Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed", "8 / 10 correct")));

        var result = tools.execute(new AgentPlanStep("GET_HISTORY", Map.of()), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Quiz attempt", "Quiz completed", "8 / 10 correct")
                .doesNotContain("GET_HISTORY", "HistoryEvent[");
        verify(history).list(36L);
    }

    @Test
    void assistantFlashcardGenerationUsesRequestedCourseMaterialAndMarksOrigin() {
        when(materials.listDocuments(36L)).thenReturn(List.of(new DocumentResponse(12L, "CPU Scheduling.pdf",
                "pdf", "application/pdf", "hash", "READY", java.time.Instant.now(), 100, 1, 1)));
        when(flashcards.generate(36L, 12L, 5, ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT))
                .thenReturn(new ca.yorku.eecs3311.coursepilot.application.dto.FlashcardGenerationResult(5, 5, List.of(), ""));

        var result = tools.execute(new AgentPlanStep("GENERATE_FLASHCARDS",
                Map.of("request", "Create 5 flashcards from document CPU Scheduling.pdf", "count", "5")), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Created 5 of 5", "ACTION|Review deck|/courses/36/flashcards/review?documentId=12");
        verify(flashcards).generate(36L, 12L, 5, ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT);
    }

    @Test
    void assistantQuizGenerationUsesRequestedMaterialAndReturnsDirectQuizAction() {
        when(materials.listDocuments(36L)).thenReturn(List.of(new DocumentResponse(12L, "CPU Scheduling.pdf",
                "pdf", "application/pdf", "hash", "READY", java.time.Instant.now(), 100, 1, 1)));
        var quiz = new ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto(91L, "CPU Scheduling quiz", 36L,
                java.time.Instant.now(), List.of());
        when(quizzes.generate(36L, 12L, 10, Set.of(), ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT))
                .thenReturn(new ca.yorku.eecs3311.coursepilot.application.dto.QuizGenerationResult(10, 10, quiz));

        var result = tools.execute(new AgentPlanStep("GENERATE_QUIZ",
                Map.of("request", "Create a quiz with 10 questions from document CPU Scheduling.pdf", "count", "10")), 36L);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Created a quiz with 10 questions", "ACTION|Take quiz|/courses/36/quizzes/91");
        verify(quizzes).generate(36L, 12L, 10, Set.of(), ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT);
    }
}
