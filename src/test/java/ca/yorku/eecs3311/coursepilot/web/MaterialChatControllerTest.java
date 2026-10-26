package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.service.GroundedQuestionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MaterialChatController.class)
class MaterialChatControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void documentChatRendersSelectedMaterialContext() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null));
        when(facade.getMaterial(1L, 7L)).thenReturn(material());
        when(facade.listMaterials(1L)).thenReturn(java.util.List.of(material()));
        mockMvc.perform(get("/courses/1/materials/7/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("StudyForge Tutor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("solid.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Split")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("My Notes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Outline")));
    }

    @Test
    void documentChatKeepsQuestionWhenAiFails() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null));
        when(facade.getMaterial(1L, 7L)).thenReturn(material());
        when(facade.listMaterials(1L)).thenReturn(java.util.List.of(material()));
        when(facade.askQuestionAboutMaterial(1L, 7L, "What is SRP?"))
                .thenThrow(new GroundedQuestionException("AI answering is currently unavailable."));
        mockMvc.perform(post("/courses/1/materials/7/chat").param("question", "What is SRP?").param("page", "4"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is SRP?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page 4 of 10")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI answering is currently unavailable.")));
    }

    private DocumentResponse material() {
        return new DocumentResponse(7L, "solid.pdf", "pdf", "application/pdf", "hash", "READY", Instant.now(), 1000, 3, 10);
    }
}
