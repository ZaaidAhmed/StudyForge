package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.Recommendation;
import ca.yorku.eecs3311.coursepilot.application.service.DuplicateCourseCodeException;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {CourseController.class, DashboardController.class})
class CourseControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;
    @MockBean ProgressService progressService;
    @MockBean RecommendationService recommendationService;

    @Test
    void courseListRenders() throws Exception {
        when(facade.listCourses()).thenReturn(List.of());
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/list"));
    }

    @Test
    void courseOverviewRendersWorkspaceAndLiveStudySummary() throws Exception {
        when(facade.getCourse(7L)).thenReturn(new CourseResponse(7L, "EECS3311", "Software Design", "Course notes", null, null, null));
        when(facade.listMaterials(7L)).thenReturn(List.of(
                new DocumentResponse(8L, "lecture.pdf", "pdf", "application/pdf", "hash", "READY", null, 100, 4, 2)));
        when(progressService.summarize(7L)).thenReturn(new ProgressSummary(7L, "Software Design", 0, 0, "NOT_STARTED", List.of()));
        when(recommendationService.recommend(7L)).thenReturn(new Recommendation(
                "REVIEW_FLASHCARDS", "Start a review to build a learning signal.", "Review flashcards", "/courses/7/flashcards"));

        mockMvc.perform(get("/courses/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Open Study Studio")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Indexed chunks")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Start a review to build a learning signal.")));
    }

    @Test
    void studyStudioOpensTheFirstReadyDocument() throws Exception {
        when(facade.listMaterials(7L)).thenReturn(List.of(
                new DocumentResponse(8L, "pending.pdf", "pdf", "application/pdf", "hash", "PROCESSING", null, 0, 0, null),
                new DocumentResponse(9L, "ready.pdf", "pdf", "application/pdf", "hash", "READY", null, 100, 4, 2),
                new DocumentResponse(10L, "later.pdf", "pdf", "application/pdf", "hash", "READY", null, 100, 4, 2)));

        mockMvc.perform(get("/courses/7/studio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/courses/7/materials/9"));
    }

    @Test
    void studyStudioSendsCoursesWithoutReadyDocumentsToTheLibrary() throws Exception {
        when(facade.listMaterials(7L)).thenReturn(List.of());

        mockMvc.perform(get("/courses/7/studio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/courses/7/materials"));
    }

    @Test
    void invalidCourseFormReturnsFormView() throws Exception {
        mockMvc.perform(post("/courses").param("courseCode", "").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/form"));
    }

    @Test
    void duplicateCourseCodeStaysOnFormWithSubmittedValues() throws Exception {
        doThrow(new DuplicateCourseCodeException("Course code 'EECS3311' already exists."))
                .when(facade).createCourse(any(CreateCourseRequest.class));

        mockMvc.perform(post("/courses")
                        .param("courseCode", "EECS3311")
                        .param("name", "Software Design")
                        .param("description", "My notes"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/form"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("already exists")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Software Design")));
    }
}
