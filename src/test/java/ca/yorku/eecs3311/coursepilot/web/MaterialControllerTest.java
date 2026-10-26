package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MaterialController.class)
class MaterialControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void materialDetailLoadsTheStudyStudioDocumentSelector() throws Exception {
        var course = new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null);
        var material = new DocumentResponse(7L, "solid.pdf", "pdf", "application/pdf", "hash",
                "READY", Instant.now(), 1000, 3, 10);
        when(facade.getCourse(1L)).thenReturn(course);
        when(facade.getMaterial(1L, 7L)).thenReturn(material);
        when(facade.listMaterials(1L)).thenReturn(List.of(material));

        mockMvc.perform(get("/courses/1/materials/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/detail"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("solid.pdf")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Split")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("studio-document-pages")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<iframe"))));
    }

    @Test
    void searchRendersResultsThroughTheMaterialsView() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.searchMaterials(1L, "strategy pattern", 5)).thenReturn(List.of(
                new SearchResult(10L, 20L, "Lecture 6.pdf", 2, "Strategy pattern text", 12.0, 3, 3, List.of("strategy", "pattern"))));

        mockMvc.perform(get("/courses/1/materials/search").param("q", "strategy pattern").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/list"));
    }

    @Test
    void blankSearchReturnsTheSameViewWithControlledValidation() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.searchMaterials(1L, "", 5)).thenThrow(new IllegalArgumentException("Search query is required."));

        mockMvc.perform(get("/courses/1/materials/search").param("q", "").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/list"));
    }

    @Test
    void failedUploadRedirectsToMaterialsWithActionableMessage() throws Exception {
        doThrow(new ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentValidationException("Only PDF, TXT, or Markdown files are supported."))
                .when(facade).importMaterial(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(multipart("/courses/1/materials")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "notes.exe", "application/octet-stream", "bad".getBytes())))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/courses/1/materials"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("errorMessage", "Only PDF, TXT, or Markdown files are supported."));
    }
}
