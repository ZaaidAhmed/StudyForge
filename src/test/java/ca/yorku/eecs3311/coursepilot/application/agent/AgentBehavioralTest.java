package ca.yorku.eecs3311.coursepilot.application.agent;

import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgentBehavioralTest {
    @Test
    void coordinatorExecutesWeakTopicPlanAndReturnsToolResultsWithoutInternalNames() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenAnswer(invocation -> {
            AgentPlanStep step = invocation.getArgument(0);
            return new AgentTool.ToolResult(step.tool(), true,
                    "GET_PROGRESS".equals(step.tool()) ? "course progress result" : "weak topics result");
        });
        MemoryManager memory = mock(MemoryManager.class);
        when(memory.recent(1L)).thenReturn(List.of());
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, memory, new ResponseSynthesizer());

        String response = coordinator.run(1L, "Help me study what I am weakest at");

        assertThat(response).contains("course progress result", "weak topics result")
                .doesNotContain("GET_PROGRESS", "GET_WEAK_TOPICS", "GENERATE_FLASHCARDS");
        verify(tools, times(2)).execute(any(), eq(1L));
        verify(memory).recent(1L);
    }

    @Test
    void boundedMemoryAddsContextToRelevantRequestsOnly() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenAnswer(invocation -> {
            AgentPlanStep step = invocation.getArgument(0);
            return new AgentTool.ToolResult(step.tool(), true, "course result");
        });
        MemoryManager memory = mock(MemoryManager.class);
        when(memory.recent(1L)).thenReturn(List.of(new HistoryEvent(
                java.time.Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "CPU Scheduling quiz completed", "4 / 10 correct")));
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, memory, new ResponseSynthesizer());

        String progressResponse = coordinator.run(1L, "Show my progress");
        String questionResponse = coordinator.run(1L, "What is a thread?");
        String generationResponse = coordinator.run(1L, "Create 5 flashcards");

        assertThat(progressResponse).contains("Recent course activity considered", "CPU Scheduling quiz completed");
        assertThat(questionResponse).contains("course result")
                .doesNotContain("Recent course activity", "CPU Scheduling quiz completed");
        assertThat(generationResponse).contains("course result")
                .doesNotContain("Recent course activity", "CPU Scheduling quiz completed");
        verify(memory, times(3)).recent(1L);
    }

    @Test
    void historyResponseUsesTheBoundedEventsPassedFromMemoryManager() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenReturn(new AgentTool.ToolResult("GET_HISTORY", true, "tool output"));
        MemoryManager memory = mock(MemoryManager.class);
        var events = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> new HistoryEvent(java.time.Instant.parse("2026-11-02T14:30:00Z").minusSeconds(index),
                        "QUIZ_ATTEMPT", "Quiz " + index, index + " / 10 correct"))
                .toList();
        when(memory.recent(1L)).thenReturn(events);
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, memory, new ResponseSynthesizer());

        String response = coordinator.run(1L, "Show my recent learning activity");

        assertThat(response).contains("Quiz 0", "Quiz 9").doesNotContain("Quiz 10", "tool output");
        verify(memory).recent(1L);
    }

    @Test
    void coordinatorPreservesActionableSanitizedToolFailure() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenReturn(new AgentTool.ToolResult("GET_PROGRESS", false, "safe failure"));
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, mock(MemoryManager.class), new ResponseSynthesizer());

        assertThat(coordinator.run(1L, "Show my progress"))
                .contains("safe failure")
                .doesNotContain("GET_PROGRESS");
    }

    @Test
    void coordinatorDoesNotRunAnyToolForUnsupportedDeletionRequest() {
        AgentToolManager tools = mock(AgentToolManager.class);
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, mock(MemoryManager.class), new ResponseSynthesizer());

        String response = coordinator.run(1L, "delete flashcards for me");

        assertThat(response).contains("can't delete saved resources", "Practice / Flashcards")
                .doesNotContain("Created", "GENERATE_FLASHCARDS");
        verifyNoInteractions(tools);
    }

    @Test
    void memoryManagerRequestsOnlyBoundedRecentHistory() {
        LearningHistoryService history = mock(LearningHistoryService.class);
        when(history.recent(7L, 10)).thenReturn(List.of());
        new MemoryManager(history).recent(7L);
        verify(history).recent(7L, 10);
        verifyNoMoreInteractions(history);
    }
}
