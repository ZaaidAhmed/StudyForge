package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private final ProgressService progress = mock(ProgressService.class);
    private final LearningHistoryService history = mock(LearningHistoryService.class);
    private final RecommendationService service = new RecommendationService(progress, history);

    @Test
    void recommendsFirstFlashcardReviewForCourseWithoutActivity() {
        when(progress.summarize(9L)).thenReturn(new ProgressSummary(9L, "Design", 0, 0, "WEAK", List.of()));
        var result = service.recommend(9L);
        assertThat(result.action()).isEqualTo("REVIEW_FLASHCARDS");
        assertThat(result.href()).isEqualTo("/courses/9/flashcards");
        verifyNoInteractions(history);
    }

    @Test
    void recommendsWeakTopicUsingTheSelectedCoursePath() {
        when(progress.summarize(9L)).thenReturn(new ProgressSummary(9L, "Design", 2, 52, "WEAK", List.of(
                new ProgressSummary.TopicProgress("SOLID", 52, "WEAK", 2))));
        var result = service.recommend(9L);
        assertThat(result.action()).isEqualTo("STUDY_TOPIC");
        assertThat(result.reason()).contains("SOLID");
        assertThat(result.href()).isEqualTo("/courses/9/materials");
    }

    @Test
    void recommendsQuizAfterRecentFlashcardReview() {
        when(progress.summarize(9L)).thenReturn(new ProgressSummary(9L, "Design", 1, 100, "STRONG", List.of(
                new ProgressSummary.TopicProgress("SOLID", 100, "STRONG", 1))));
        when(history.recent(9L, 1)).thenReturn(List.of(new HistoryEvent(Instant.now(), "FLASHCARD_REVIEW", "Review", "Good")));
        var result = service.recommend(9L);
        assertThat(result.action()).isEqualTo("TAKE_QUIZ");
        assertThat(result.href()).isEqualTo("/courses/9/quizzes");
    }
}
