package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardSourceResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(FlashcardController.class)
class FlashcardControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void missingGenerationInputsStayOnPageWithRecoveryMessage() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listFlashcards(1L)).thenReturn(List.of());

        mockMvc.perform(post("/courses/1/flashcards/generate"))
                .andExpect(status().isOk())
                .andExpect(view().name("flashcards/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please select a ready document.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload material")));
    }

    @Test
    void reviewPageStartsWithOneQuestionAndExposesTheThreeSelfRatings() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listFlashcards(1L)).thenReturn(List.of(
                new FlashcardResponse(7L, 1L, "Question one?", "Answer one.", null, List.of()),
                new FlashcardResponse(8L, 1L, "Question two?", "Answer two.", null, List.of())));

        mockMvc.perform(get("/courses/1/flashcards/review"))
                .andExpect(status().isOk())
                .andExpect(view().name("flashcards/review"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Question one?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Learning")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Familiar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mastered")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-current-index=\"0\"")));
    }

    @Test
    void savingARatingRedirectsToTheNextCard() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listFlashcards(1L)).thenReturn(List.of(
                new FlashcardResponse(7L, 1L, "Question one?", "Answer one.", null, List.of()),
                new FlashcardResponse(8L, 1L, "Question two?", "Answer two.", null, List.of())));

        mockMvc.perform(post("/courses/1/flashcards/7/review")
                        .param("cardIndex", "0")
                        .param("rating", "HARD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/flashcards/review?cardIndex=1"));

        verify(facade).reviewFlashcard(1L, 7L, "HARD");
    }

    @Test
    void completingTheDeckShowsTheCompletionState() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listFlashcards(1L)).thenReturn(List.of(
                new FlashcardResponse(7L, 1L, "Question one?", "Answer one.", null, List.of())));

        mockMvc.perform(get("/courses/1/flashcards/review").param("cardIndex", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("flashcards/review"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You reviewed every card in this course.")));
    }

    @Test
    void ratingCannotBeSubmittedForAnotherCardPosition() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listFlashcards(1L)).thenReturn(List.of(
                new FlashcardResponse(7L, 1L, "Question one?", "Answer one.", null, List.of())));

        mockMvc.perform(post("/courses/1/flashcards/7/review")
                        .param("cardIndex", "1")
                        .param("rating", "GOOD"))
                .andExpect(status().isOk())
                .andExpect(view().name("flashcards/review"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "The current card does not match this review position.")));

        verify(facade, never()).reviewFlashcard(anyLong(), anyLong(), anyString());
    }

    @Test
    void documentDeckReviewUsesOnlyTheSelectedDocument() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.getMaterial(1L, 9L)).thenReturn(new DocumentResponse(9L, "patterns.pdf", "pdf", "application/pdf", "hash", "READY", null, 100, 2, 4));
        when(facade.listFlashcards(1L, 9L)).thenReturn(List.of(new FlashcardResponse(7L, 1L, "Pattern?", "A reusable solution.", null,
                List.of(new FlashcardSourceResponse(9L, "patterns.pdf", 11L, 0, 1, 1)))));

        mockMvc.perform(get("/courses/1/flashcards/review").param("documentId", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reviewing:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("patterns.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pattern?")));
        verify(facade).listFlashcards(1L, 9L);
        verify(facade, never()).listFlashcards(1L);
    }

    @Test
    void selectedDeckPageShowsOnlyTheRequestedDocumentWorkspace() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.getMaterial(1L, 9L)).thenReturn(new DocumentResponse(9L, "patterns.pdf", "pdf", "application/pdf", "hash", "READY", null, 100, 2, 4));
        FlashcardSourceResponse source = new FlashcardSourceResponse(9L, "patterns.pdf", 11L, 0, 1, 1);
        FlashcardResponse card = new FlashcardResponse(7L, 1L, "Pattern?", "A reusable solution.", null, List.of(source));
        when(facade.listFlashcards(1L)).thenReturn(List.of(card));

        mockMvc.perform(get("/courses/1/flashcards").param("documentId", "9"))
                .andExpect(status().isOk())
                .andExpect(view().name("flashcards/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Selected deck")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("patterns.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Start review")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Search this deck")));
        verify(facade).getMaterial(1L, 9L);
    }

    @Test
    void deleteFlashcardRedirectsToDeckPage() throws Exception {
        mockMvc.perform(post("/courses/1/flashcards/7/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/flashcards"));
        verify(facade).deleteFlashcard(1L, 7L);
    }

    @Test
    void deleteDeckRedirectsToFlashcardsPage() throws Exception {
        mockMvc.perform(post("/courses/1/flashcards/deck/delete").param("documentId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/flashcards"));
        verify(facade).deleteFlashcardDeck(1L, 9L);
    }
}
