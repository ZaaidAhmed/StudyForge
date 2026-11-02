package ca.yorku.eecs3311.coursepilot.application.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {
    @Test
    void keepsPromptSectionsExplicitAndSeparate() {
        LlmRequest request = new PromptBuilder().build("Be concise.", "Explain the term.", "A definition.", "Return one paragraph.");
        assertThat(request.systemPrompt()).isEqualTo("Be concise.");
        assertThat(request.userPrompt()).contains("TASK:\nExplain the term.", "CONTEXT:\nA definition.", "OUTPUT REQUIREMENTS:\nReturn one paragraph.");
    }

    @Test
    void flashcardPromptRequiresConceptsAndTypedRetrievalCards() {
        LlmRequest request = new PromptBuilder().buildFlashcardFromConceptsPrompt(5, "notes.pdf", "CONCEPT C1\nName: Strategy\nDefinition: Selects an algorithm at runtime.\nSource IDs: S1");

        assertThat(request.userPrompt()).contains("exactly 5 high-quality flashcards", "EXTRACTED CONCEPTS", "self-contained", "type");
        assertThat(request.temperature()).isEqualTo(0.05d);
    }

    @Test
    void replacementPromptRequiresUnusedLearningAnglesAndExactTarget() {
        LlmRequest request = new PromptBuilder().buildFlashcardReplacementPrompt(
                1, "notes.pdf", "CONCEPT C1: Thread", "A1 concept=Thread type=definition objective=definition of thread",
                java.util.List.of("What is a thread?"));

        assertThat(request.userPrompt()).contains("exactly 1 new flashcards", "UNUSED LEARNING ANGLES",
                "CARDS ALREADY COVERED", "learning objective", "sourceIds");
    }

    @Test
    void quizPromptRequiresConceptBasedProfessorStyleQuestions() {
        LlmRequest request = new PromptBuilder().buildQuizGenerationPromptFromConcepts(
                5, "MULTIPLE_CHOICE,TRUE_FALSE", "notes.pdf",
                "CONCEPT: Single Responsibility Principle\nDEFINITION: A class should have one reason to change.\nSOURCE IDS: S1",
                java.util.List.of("S1"), java.util.List.of());

        assertThat(request.systemPrompt()).contains("university assessment writer");
        assertThat(request.userPrompt()).contains("EXTRACTED CONCEPTS", "exactly 4 unique plausible options",
                "scenario-based reasoning", "without seeing any source file");
        assertThat(request.userPrompt()).doesNotContain("SOURCE MATERIAL:");
    }

    @Test
    void quizRecoveryPromptIncludesTargetAndStrictSchema() {
        LlmRequest request = new PromptBuilder().buildQuizRecoveryPrompt(
                2, "MULTIPLE_CHOICE,TRUE_FALSE", "notes.pdf", "CONCEPT: Thread",
                java.util.List.of("S1"), java.util.List.of("What is a thread?"), "invalid JSON");
        assertThat(request.userPrompt()).contains("Create 2", "invalid JSON", "S1", "questions", "Never mention a document");
        assertThat(request.structuredOutputExpected()).isTrue();
    }
}
