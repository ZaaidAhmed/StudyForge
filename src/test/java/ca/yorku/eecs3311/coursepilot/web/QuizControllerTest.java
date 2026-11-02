package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizGenerationResult;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizQuestionResponse;
import ca.yorku.eecs3311.coursepilot.domain.model.QuizQuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(QuizController.class)
class QuizControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;

    @Test
    void invalidCountPreservesSubmittedSelectionAndShowsInlineRecovery() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listQuizzes(1L)).thenReturn(List.of());
        mockMvc.perform(post("/courses/1/quizzes/generate").param("documentId", "7").param("count", "21"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("between 1 and 20")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"21\"")));
    }

    @Test
    void successfulGenerationShowsNewQuizAndStartLinkImmediately() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        QuizResponseDto quiz = new QuizResponseDto(42L, "notes.pdf quiz", 1L, Instant.parse("2026-10-12T00:00:00Z"), List.of());
        when(facade.listQuizzes(1L)).thenReturn(List.of()).thenReturn(List.of(quiz));
        when(facade.generateQuiz(1L, 7L, 5, Set.of("MULTIPLE_CHOICE", "TRUE_FALSE")))
                .thenReturn(new QuizGenerationResult(5, 3, quiz));

        mockMvc.perform(post("/courses/1/quizzes/generate").param("documentId", "7").param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("3 questions generated and saved.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/1/quizzes/42")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notes.pdf quiz")));
        verify(facade, times(2)).listQuizzes(1L);
    }

    @Test
    void selectedShortAnswerTypeReachesGenerationService() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        when(facade.listMaterials(1L)).thenReturn(List.of());
        when(facade.listQuizzes(1L)).thenReturn(List.of()).thenReturn(List.of());
        QuizResponseDto quiz = new QuizResponseDto(43L, "notes.pdf quiz", 1L, Instant.parse("2026-10-12T00:00:00Z"), List.of());
        when(facade.generateQuiz(1L, 7L, 5, Set.of("MULTIPLE_CHOICE", "SHORT_ANSWER")))
                .thenReturn(new QuizGenerationResult(5, 5, quiz));

        mockMvc.perform(post("/courses/1/quizzes/generate")
                        .param("documentId", "7")
                        .param("count", "5")
                        .param("questionTypes", "MULTIPLE_CHOICE", "SHORT_ANSWER"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/index"));
        verify(facade).generateQuiz(1L, 7L, 5, Set.of("MULTIPLE_CHOICE", "SHORT_ANSWER"));
    }

    @Test
    void deleteQuizRedirectsToCourseScopedQuizList() throws Exception {
        doNothing().when(facade).deleteQuiz(1L, 42L);

        mockMvc.perform(post("/courses/1/quizzes/42/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/courses/1/quizzes"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("successMessage", "Quiz was deleted."));

        verify(facade).deleteQuiz(1L, 42L);
    }

    @Test
    void malformedQuestionParameterStaysOnQuizPageWithInlineError() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        QuizResponseDto quiz = new QuizResponseDto(42L, "notes.pdf quiz", 1L,
                Instant.parse("2026-10-12T00:00:00Z"), List.of());
        when(facade.getQuiz(1L, 42L)).thenReturn(quiz);

        mockMvc.perform(post("/courses/1/quizzes/42/submit").param("q-not-a-number", "answer"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/take"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Quiz answers contain an invalid question identifier.")));
    }

    @Test
    void quizRendersAllQuestionsInOneFullForm() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        QuizQuestionResponse first = new QuizQuestionResponse(11L, QuizQuestionType.MULTIPLE_CHOICE,
                "What is cohesion?", List.of("A", "B", "C", "D"), "A", "Cohesion groups related responsibilities.", List.of());
        QuizQuestionResponse second = new QuizQuestionResponse(12L, QuizQuestionType.TRUE_FALSE,
                "Concurrency is parallelism.", List.of(), "false", "Concurrency can exist without parallel execution.", List.of());
        when(facade.getQuiz(1L, 42L)).thenReturn(new QuizResponseDto(42L, "Design quiz", 1L,
                Instant.parse("2026-10-12T00:00:00Z"), List.of(first, second)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/courses/1/quizzes/42"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Question 1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is cohesion?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Concurrency is parallelism.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Next question"))));
    }

    @Test
    void fullQuizSubmissionShowsResultStateAndPreservesQuestionOrder() throws Exception {
        when(facade.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "Course", null, null, null));
        QuizQuestionResponse first = new QuizQuestionResponse(11L, QuizQuestionType.MULTIPLE_CHOICE,
                "What is cohesion?", List.of("A", "B", "C", "D"), "A", "Cohesion groups related responsibilities.", List.of());
        QuizQuestionResponse second = new QuizQuestionResponse(12L, QuizQuestionType.TRUE_FALSE,
                "Concurrency is parallelism.", List.of(), "false", "Concurrency can exist without parallel execution.", List.of());
        when(facade.getQuiz(1L, 42L)).thenReturn(new QuizResponseDto(42L, "Design quiz", 1L,
                Instant.parse("2026-10-12T00:00:00Z"), List.of(first, second)));
        when(facade.submitQuiz(1L, 42L, java.util.Map.of(11L, "B", 12L, "false")))
                .thenReturn(new ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptResult(
                        9L, 42L, 1, 2, List.of(
                        new ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptResult.AnswerResult(11L, "B", false, "Review cohesion."),
                        new ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptResult.AnswerResult(12L, "false", true, "Correct."))));

        mockMvc.perform(post("/courses/1/quizzes/42/submit")
                        .param("q-11", "B").param("q-12", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/take"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Score: 1 / 2")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Correct answer:"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Explain this answer")));
    }
}
