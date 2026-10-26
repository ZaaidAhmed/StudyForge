package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.Recommendation;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.RecommendationService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CourseWorkspaceController.class)
class CourseWorkspaceControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;
    @MockBean ProgressService progressService;
    @MockBean RecommendationService recommendationService;
    @MockBean StudyPlanService studyPlanService;
    @MockBean LearningHistoryService historyService;

    @Test
    void toolsHubKeepsTheCourseWorkflowTogether() throws Exception {
        stubCourseAndMaterials();

        mockMvc.perform(get("/courses/1/tools"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/tools"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Materials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/studio")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/practice")));
    }

    @Test
    void practiceHubLinksToBothPracticeModesWithCourseContext() throws Exception {
        stubCourseAndMaterials();
        when(facade.listFlashcards(1L)).thenReturn(List.of());
        when(facade.listQuizzes(1L)).thenReturn(List.of());

        mockMvc.perform(get("/courses/1/practice"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/practice"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/flashcards")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/quizzes")));
    }

    @Test
    void insightsHubExposesProgressAndHistory() throws Exception {
        stubCourseAndMaterials();
        when(progressService.summarize(1L)).thenReturn(new ProgressSummary(1L, "Software Design", 0, 0, "NOT_STARTED", List.of()));
        when(recommendationService.recommend(1L)).thenReturn(new Recommendation("REVIEW_FLASHCARDS", "Start reviewing.", "Review flashcards", "/courses/1/flashcards"));
        when(studyPlanService.list(1L)).thenReturn(List.of());
        when(historyService.list(1L)).thenReturn(List.of());

        mockMvc.perform(get("/courses/1/insights"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/insights"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/progress")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/history")));
    }

    private void stubCourseAndMaterials() {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", "Notes", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of(
                new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "READY", null, 120, 3, 4)));
    }
}
