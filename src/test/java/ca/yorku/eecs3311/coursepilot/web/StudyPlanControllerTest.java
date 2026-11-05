package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanService;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudyPlanController.class)
class StudyPlanControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;
    @MockBean StudyPlanService plans;
    @MockBean ProgressService progress;
    @MockBean StudyPlanReviewService reviewQueue;

    @Test
    void studyPlanRendersDerivedSummaryAndEmptyState() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", "Description", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(plans.list(1L)).thenReturn(List.of());
        when(progress.summarize(1L)).thenReturn(new ProgressSummary(1L, "Software Design", 0, 0, "WEAK", List.of()));
        when(reviewQueue.build(1L, "Software Design")).thenReturn(List.of());

        mockMvc.perform(get("/courses/1/study-plan"))
                .andExpect(status().isOk())
                .andExpect(view().name("study-plan/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Planned workload")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Not started")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No study plans yet")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Generate AI Study Plan")));
    }

    @Test
    void malformedStudyPlanReturnsInlineRecoveryMessage() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", "Description", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(plans.list(1L)).thenReturn(List.of());
        when(progress.summarize(1L)).thenReturn(new ProgressSummary(1L, "Software Design", 0, 0, "WEAK", List.of()));
        when(reviewQueue.build(1L, "Software Design")).thenReturn(List.of());

        mockMvc.perform(post("/courses/1/study-plan")
                        .param("startDate", "bad")
                        .param("targetDate", "2026-12-10")
                        .param("minutes", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("study-plan/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dates must use the YYYY-MM-DD format.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"bad\"")));
    }
}
