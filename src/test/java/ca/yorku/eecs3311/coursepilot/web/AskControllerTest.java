package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedAnswer;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource;
import ca.yorku.eecs3311.coursepilot.application.service.GroundedQuestionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {AskController.class, AiStatusController.class})
class AskControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void askFormKeepsCourseContext() throws Exception {
        when(facade.getCourse(1L)).thenReturn(course());
        mockMvc.perform(get("/courses/1/ask"))
                .andExpect(status().isOk())
                .andExpect(view().name("ask/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ask about this course")));
    }

    @Test
    void unavailableAiReturnsQuestionAndRecoveryMessage() throws Exception {
        when(facade.getCourse(1L)).thenReturn(course());
        when(facade.askQuestion(1L, "What is strategy?")).thenThrow(new GroundedQuestionException("AI answering is currently unavailable."));
        mockMvc.perform(post("/courses/1/ask").param("question", "What is strategy?"))
                .andExpect(status().isOk())
                .andExpect(view().name("ask/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is strategy?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check AI status")));
    }

    @Test
    void groundedAnswerDisplaysSourceMetadata() throws Exception {
        when(facade.getCourse(1L)).thenReturn(course());
        when(facade.askQuestion(1L, "What is strategy?")).thenReturn(new GroundedAnswer("What is strategy?", "It defines interchangeable algorithms.", List.of(new GroundedSource("S1", 10L, "Lecture.pdf", 20L, 2, 12, 12, "Supporting text")), true, false));
        mockMvc.perform(post("/courses/1/ask").param("question", "What is strategy?"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("It defines interchangeable algorithms.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lecture.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page 12")));
    }

    @Test
    void aiStatusPageRendersAvailability() throws Exception {
        when(facade.aiStatus()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test-model", "reachable"));
        mockMvc.perform(get("/ai/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("ai/status"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test-model")));
    }

    private CourseResponse course() {
        return new CourseResponse(1L, "EECS3311", "Software Design", "Course description", null, null, null);
    }
}
