package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentControllerTest {
    private StudyAgentFacade facade;
    private ProgressService progress;
    private StudyPlanReviewService reviewQueue;
    private LearningHistoryService history;
    private AgentController controller;
    private MockHttpSession session;
    private ExtendedModelMap model;

    private final List<DocumentResponse> twoReadyMaterials = List.of(
            document(11L, "Threads and Processes.pdf", "READY"),
            document(12L, "CPU Scheduling.pdf", "READY"));

    @BeforeEach
    void setUp() {
        facade = mock(StudyAgentFacade.class);
        progress = mock(ProgressService.class);
        reviewQueue = mock(StudyPlanReviewService.class);
        history = mock(LearningHistoryService.class);
        controller = new AgentController(facade, progress, reviewQueue, history);
        session = new MockHttpSession();
        model = new ExtendedModelMap();

        when(facade.getCourse(36L)).thenReturn(new CourseResponse(36L, "EECS3221", "Operating Systems",
                "", null, Instant.now(), Instant.now()));
        when(facade.listMaterials(36L)).thenReturn(twoReadyMaterials);
        when(facade.listQuizzes(36L)).thenReturn(List.of());
        when(facade.listFlashcards(36L)).thenReturn(List.of());
        when(progress.summarize(36L)).thenReturn(new ProgressSummary(36L, "Operating Systems", 0, 0,
                "NOT_STARTED", List.of()));
        when(reviewQueue.build(36L, "Operating Systems")).thenReturn(List.of());
        when(history.recent(36L, 1)).thenReturn(List.of());
    }

    @Test
    void asksWhichMaterialBeforeSummarizingAnAmbiguousRequest() {
        String view = controller.ask(36L, "Summarize my course materials", session, model);

        assertThat(view).isEqualTo("agent/index");
        assertThat(lastResponse()).contains("Which should I summarize?", "1. Threads and Processes.pdf",
                "2. CPU Scheduling.pdf", "all materials");
        verify(facade, never()).agentAssist(eq(36L), anyString());
    }

    @Test
    void explicitAllChoiceResolvesPendingQuestionAndRunsCourseSummary() {
        when(facade.agentAssist(36L, "Summarize all my course materials"))
                .thenReturn("Combined course summary");
        controller.ask(36L, "Summarize my course materials", session, model);
        model = new ExtendedModelMap();

        controller.ask(36L, "All materials", session, model);

        verify(facade).agentAssist(36L, "Summarize all my course materials");
        assertThat(lastResponse()).contains("Combined course summary");
    }

    @Test
    void materialNameResolvesPendingQuestionToThatDocument() {
        controller.ask(36L, "Summarize my course materials", session, model);
        model = new ExtendedModelMap();

        controller.ask(36L, "Threads and Processes.pdf", session, model);

        verify(facade).agentAssist(36L, "Summarize document: Threads and Processes.pdf");
    }

    @Test
    void ordinalResolvesPendingQuestionAndUnclearReplyAsksAgain() {
        when(facade.agentAssist(36L, "Summarize document: CPU Scheduling.pdf"))
                .thenReturn("CPU Scheduling summary");
        controller.ask(36L, "Summarize my course materials", session, model);
        model = new ExtendedModelMap();
        controller.ask(36L, "I'm not sure", session, model);
        assertThat(lastResponse()).contains("Which should I summarize?");
        verify(facade, never()).agentAssist(eq(36L), anyString());

        model = new ExtendedModelMap();
        controller.ask(36L, "the second one", session, model);
        verify(facade).agentAssist(36L, "Summarize document: CPU Scheduling.pdf");
    }

    @Test
    void oneReadyMaterialDoesNotRequireClarification() {
        when(facade.listMaterials(36L)).thenReturn(List.of(document(11L, "Threads and Processes.pdf", "READY")));
        when(facade.agentAssist(36L, "Summarize my course materials")).thenReturn("Single document summary");

        controller.ask(36L, "Summarize my course materials", session, model);

        verify(facade).agentAssist(36L, "Summarize my course materials");
        assertThat(lastResponse()).contains("Single document summary").doesNotContain("Which should I summarize?");
    }

    @Test
    void explicitAllRequestSkipsClarificationWhenSeveralMaterialsExist() {
        when(facade.agentAssist(36L, "Summarize all my course materials")).thenReturn("Combined course summary");

        controller.ask(36L, "Summarize all my course materials", session, model);

        verify(facade).agentAssist(36L, "Summarize all my course materials");
        assertThat(lastResponse()).contains("Combined course summary").doesNotContain("Which should I summarize?");
    }

    @Test
    void explicitMaterialNameSkipsClarificationAndKeepsTheRequestedScope() {
        when(facade.agentAssist(36L, "Summarize document: Threads and Processes.pdf"))
                .thenReturn("Threads summary");

        controller.ask(36L, "Summarize Threads and Processes", session, model);

        verify(facade).agentAssist(36L, "Summarize document: Threads and Processes.pdf");
        assertThat(lastResponse()).contains("Threads summary").doesNotContain("Which should I summarize?");
    }

    @Test
    void rendersRecentHistoryAsStructuredCourseActivity() {
        when(facade.agentAssist(36L, "Show my recent learning history."))
                .thenReturn("Here is your recent learning activity.\n\n- Nov 2, 2026 at 6:45 PM | Study plan: Study plan created | Weak-topic focused");
        when(history.list(36L)).thenReturn(List.of(new HistoryEvent(
                Instant.parse("2026-11-02T23:45:00Z"), "STUDY_PLAN", "Study plan created",
                "Weak-topic focused | 120 min planned | 4 sessions", "Weak-topic focused",
                "Nov 2 to Nov 19 | 120 min planned | 4 sessions", "Open study plans", "/courses/36/study-plan")));

        controller.ask(36L, "Show my recent learning history.", session, model);

        @SuppressWarnings("unchecked")
        List<ca.yorku.eecs3311.coursepilot.application.dto.AgentChatEntry> entries =
                (List<ca.yorku.eecs3311.coursepilot.application.dto.AgentChatEntry>) model.get("assistantMessages");
        var reply = entries.getLast();
        assertThat(reply.result()).contains("1 latest course activity");
        assertThat(reply.historyEvents()).hasSize(1);
        assertThat(reply.historyEvents().getFirst().primaryDetail()).isEqualTo("Weak-topic focused");
        assertThat(reply.historyEvents().getFirst().actionUrl()).isEqualTo("/courses/36/study-plan");
    }

    @Test
    void generatedContentRequiresMaterialCountAndExplicitConfirmation() {
        controller.ask(36L, "Create 5 flashcards from CPU Scheduling.pdf", session, model);
        assertThat(lastResponse()).contains("Ready to create 5 flashcards", "CPU Scheduling.pdf", "Reply 'yes'");
        verify(facade, never()).agentAssist(eq(36L), anyString());

        model = new ExtendedModelMap();
        controller.ask(36L, "yes", session, model);

        verify(facade).agentAssist(36L, "Create a 5 flashcards from document CPU Scheduling.pdf");
    }

    @Test
    void generationClarificationAsksForDocumentAndCountAndDoesNotTreatDocumentIndexAsCount() {
        controller.ask(36L, "Create a quiz", session, model);
        assertThat(lastResponse()).contains("Which course material", "1. Threads and Processes.pdf", "2. CPU Scheduling.pdf");

        model = new ExtendedModelMap();
        controller.ask(36L, "second", session, model);
        assertThat(lastResponse()).contains("How many questions", "CPU Scheduling.pdf");

        model = new ExtendedModelMap();
        controller.ask(36L, "10", session, model);
        assertThat(lastResponse()).contains("Ready to create 10 quiz questions", "CPU Scheduling.pdf", "Reply 'yes'");
        verify(facade, never()).agentAssist(eq(36L), anyString());
    }

    @Test
    void cancellingGenerationClearsTheUnifiedPendingState() {
        controller.ask(36L, "Make a quiz", session, model);
        model = new ExtendedModelMap();
        controller.ask(36L, "cancel", session, model);
        assertThat(lastResponse()).contains("cancelled");
        model = new ExtendedModelMap();
        controller.ask(36L, "10", session, model);
        assertThat(lastResponse()).doesNotContain("How many", "Which course material");
    }

    @Test
    void pendingClarificationIsCourseScopedAndCanBeCancelled() {
        when(facade.getCourse(37L)).thenReturn(new CourseResponse(37L, "EECS3311", "Software Design",
                "", null, Instant.now(), Instant.now()));
        when(facade.listMaterials(37L)).thenReturn(List.of());
        when(facade.listQuizzes(37L)).thenReturn(List.of());
        when(facade.listFlashcards(37L)).thenReturn(List.of());
        when(progress.summarize(37L)).thenReturn(new ProgressSummary(37L, "Software Design", 0, 0,
                "NOT_STARTED", List.of()));
        when(reviewQueue.build(37L, "Software Design")).thenReturn(List.of());
        when(history.recent(37L, 1)).thenReturn(List.of());
        when(facade.agentAssist(37L, "What is a process?")).thenReturn("A process is a program in execution.");
        controller.ask(36L, "Summarize my course materials", session, model);
        model = new ExtendedModelMap();
        controller.ask(37L, "What is a process?", session, model);
        verify(facade).agentAssist(37L, "What is a process?");

        model = new ExtendedModelMap();
        controller.ask(36L, "Cancel", session, model);
        assertThat(lastResponse()).contains("cancelled");
        verify(facade, never()).agentAssist(eq(36L), anyString());
    }

    private String lastResponse() {
        @SuppressWarnings("unchecked")
        List<ca.yorku.eecs3311.coursepilot.application.dto.AgentChatEntry> entries =
                (List<ca.yorku.eecs3311.coursepilot.application.dto.AgentChatEntry>) model.get("assistantMessages");
        return entries.getLast().result();
    }

    private DocumentResponse document(Long id, String name, String status) {
        return new DocumentResponse(id, name, "pdf", "application/pdf", "hash", status,
                Instant.now(), 100, 1, 1);
    }
}
