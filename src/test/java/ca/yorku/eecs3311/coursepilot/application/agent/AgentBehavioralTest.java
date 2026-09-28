package ca.yorku.eecs3311.coursepilot.application.agent;

import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgentBehavioralTest {
    @Test
    void coordinatorExecutesObservableWeakTopicPlanAndIncludesToolResults() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenAnswer(invocation -> {
            AgentPlanStep step = invocation.getArgument(0);
            return new AgentTool.ToolResult(step.tool(), true, "result-for-" + step.tool());
        });
        MemoryManager memory = mock(MemoryManager.class);
        when(memory.recent(1L)).thenReturn(List.of());
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, memory, new ResponseSynthesizer());

        String response = coordinator.run(1L, "Help me study what I am weakest at");

        assertThat(response).contains("GET_PROGRESS", "GET_WEAK_TOPICS", "GENERATE_FLASHCARDS", "result-for-");
        verify(tools, times(3)).execute(any(), eq(1L));
        verify(memory).recent(1L);
    }

    @Test
    void coordinatorSurfacesToolFailureInsteadOfSilentlyIgnoringIt() {
        AgentToolManager tools = mock(AgentToolManager.class);
        when(tools.execute(any(), eq(1L))).thenReturn(new AgentTool.ToolResult("GET_PROGRESS", false, "safe failure"));
        AgentCoordinator coordinator = new AgentCoordinator(new IntentAnalyzer(), new TaskPlanner(),
                new AgentPlanValidator(), tools, mock(MemoryManager.class), new ResponseSynthesizer());

        assertThat(coordinator.run(1L, "Show my progress")).contains("⚠ GET_PROGRESS", "safe failure");
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
