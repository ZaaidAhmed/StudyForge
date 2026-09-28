package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {CourseController.class, DashboardController.class})
class CourseControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void courseListRenders() throws Exception {
        when(facade.listCourses()).thenReturn(List.of());
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/list"));
    }

    @Test
    void invalidCourseFormReturnsFormView() throws Exception {
        mockMvc.perform(post("/courses").param("courseCode", "").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/form"));
    }
}
