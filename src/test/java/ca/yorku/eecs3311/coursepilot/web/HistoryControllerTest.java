package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HistoryController.class)
class HistoryControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudyAgentFacade facade;
    @MockBean LearningHistoryService historyService;

    @Test
    void rendersCourseScopedEmptyStateAndHelpfulActions() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of());

        mockMvc.perform(get("/courses/36/history").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("history/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No learning history yet")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/36/practice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/36/study-plan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Operating Systems")));
    }

    @Test
    void rendersReadableEscapedTimelineEventsAndCourseLink() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(
                new HistoryEvent(Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed",
                        "8 / 10 correct | <script>alert(1)</script>")));

        mockMvc.perform(get("/courses/36/history").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quiz attempt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nov 2, 2026 at")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/36")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))));
    }

    @Test
    void rendersEventSpecificSummaryAndCourseScopedAction() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(new HistoryEvent(
                Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed",
                "8 of 10 correct · Threads quiz", "Threads quiz", "8 of 10 correct",
                "Review attempts", "/courses/36/quizzes/72/attempts")));

        mockMvc.perform(get("/courses/36/history").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Threads quiz")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("8 of 10 correct")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/courses/36/quizzes/72/attempts\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Review attempts")));
    }

    @Test
    void filtersByActivityTypeAndKeepsSummaryCountsForTheWholeCourseHistory() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(
                new HistoryEvent(Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed", "8 / 10 correct"),
                new HistoryEvent(Instant.parse("2026-11-01T14:30:00Z"), "FLASHCARD_REVIEW", "Flashcard reviewed", "GOOD | Thread"),
                new HistoryEvent(Instant.parse("2026-10-30T14:30:00Z"), "STUDY_PLAN", "Study plan created", "BALANCED | 4 activities")));

        mockMvc.perform(get("/courses/36/history").param("type", "quiz_attempt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quiz attempt")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Flashcard reviewed"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Study plan created"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quiz attempts")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/36/history?type=FLASHCARD_REVIEW")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("All activity")));
    }

    @Test
    void rendersDateGroupsAndNoMatchRecoveryForSelectedFilter() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(
                new HistoryEvent(Instant.parse("2026-11-02T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed", "8 / 10 correct"),
                new HistoryEvent(Instant.parse("2026-11-01T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed", "7 / 10 correct")));

        mockMvc.perform(get("/courses/36/history").param("type", "STUDY_PLAN"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No matching activity")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Show all activity")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/courses/36/history?type=ALL")));

        mockMvc.perform(get("/courses/36/history").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Monday, November 2, 2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sunday, November 1, 2026")));
    }

    @Test
    void keepsActivityDetailsCollapsedUntilAnActivityTabIsSelected() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(new HistoryEvent(
                Instant.parse("2026-11-02T14:30:00Z"), "FLASHCARD_REVIEW", "Flashcard reviewed",
                "Rated Good: What is a thread?", "What is a thread?", "Rated Good",
                "Review deck", "/courses/36/flashcards/review?documentId=72", "Threads.pdf")));

        mockMvc.perform(get("/courses/36/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose an activity tab to explore your history")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Flashcard reviewed"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("history-timeline-list"))));

        mockMvc.perform(get("/courses/36/history").param("type", "FLASHCARD_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Threads.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is a thread?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("history-review-deck")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<details class=\"history-review-deck\" open"))));
    }

    @Test
    void groupsFlashcardReviewEventsIntoSeparateDocumentDecks() throws Exception {
        when(facade.getCourse(36L)).thenReturn(course());
        when(historyService.list(36L)).thenReturn(List.of(
                new HistoryEvent(Instant.parse("2026-11-03T20:30:00Z"), "FLASHCARD_REVIEW", "Flashcard reviewed",
                        "Rated Good: What is a thread?", "What is a thread?", "Rated Good", "Review deck",
                        "/courses/36/flashcards/review?documentId=72", "Threads.pdf"),
                new HistoryEvent(Instant.parse("2026-11-02T14:30:00Z"), "FLASHCARD_REVIEW", "Flashcard reviewed",
                        "Rated Hard: What is a process?", "What is a process?", "Rated Hard", "Review deck",
                        "/courses/36/flashcards/review?documentId=73", "Processes.pdf"),
                new HistoryEvent(Instant.parse("2026-10-30T14:30:00Z"), "QUIZ_ATTEMPT", "Quiz completed", "8 / 10 correct"),
                new HistoryEvent(Instant.parse("2026-11-01T14:30:00Z"), "FLASHCARD_REVIEW", "Flashcard reviewed",
                        "Rated Hard: What is a process?", "What is a process?", "Rated Hard", "Review deck",
                        "/courses/36/flashcards/review?documentId=73", "Processes.pdf")));

        var chronological = mockMvc.perform(get("/courses/36/history").param("type", "FLASHCARD_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Threads.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Latest review: Tuesday, November 3, 2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Processes.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is a thread?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is a process?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("documentId=72")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("documentId=73")))
                .andReturn();
        String chronologyHtml = chronological.getResponse().getContentAsString();
        Assertions.assertTrue(chronologyHtml.indexOf("Monday, November 2, 2026")
                < chronologyHtml.indexOf("Sunday, November 1, 2026"));

        var allActivity = mockMvc.perform(get("/courses/36/history").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Threads.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Processes.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"history-timeline-item\"")))
                .andReturn();
        String allActivityHtml = allActivity.getResponse().getContentAsString();
        Assertions.assertTrue(allActivityHtml.indexOf("class=\"history-deck-section\"")
                < allActivityHtml.indexOf("class=\"history-timeline-item\""),
                "the deck reviewed most recently should appear before older activity");
    }

    private CourseResponse course() {
        return new CourseResponse(36L, "EECS3221", "Operating Systems", "Notes", null, null, null);
    }
}
