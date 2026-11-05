package ca.yorku.eecs3311.coursepilot.application.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentPlanningTest {
    private final IntentAnalyzer analyzer = new IntentAnalyzer();

    @Test
    void weaknessRequestCreatesTwoStepCourseScopedPlan() {
        var intent = analyzer.analyze("Help me study what I am weakest at");
        var plan = new TaskPlanner().plan(intent);
        assertEquals(AgentIntent.WEAK_TOPIC_SUPPORT, intent);
        assertEquals(2, plan.steps().size());
    }

    @Test
    void deletionRequestCannotBeMistakenForFlashcardGeneration() {
        assertEquals(AgentIntent.UNSUPPORTED_REQUEST, analyzer.analyze("delete flashcards for me"));
        assertEquals(AgentIntent.UNSUPPORTED_REQUEST, analyzer.analyze("Remove the saved quiz"));
    }

    @Test
    void subjectQuestionUsesGroundedCourseQuestionTool() {
        var intent = analyzer.analyze("What is a thread in an operating system?");
        var plan = new TaskPlanner().plan(intent, "What is a thread in an operating system?");
        assertEquals(AgentIntent.COURSE_QUESTION, intent);
        assertEquals("ANSWER_COURSE_QUESTION", plan.steps().getFirst().tool());
        assertEquals("What is a thread in an operating system?", plan.steps().getFirst().parameters().get("question"));
    }

    @Test
    void summaryPlanPreservesTheRequestedMaterialScope() {
        String request = "Summarize document: CPU Scheduling.pdf";
        var plan = new TaskPlanner().plan(analyzer.analyze(request), request);

        assertEquals("GENERATE_SUMMARY", plan.steps().getFirst().tool());
        assertEquals(request, plan.steps().getFirst().parameters().get("request"));
    }

    @Test
    void mentionsOfFlashcardsAreNotEnoughToTriggerGeneration() {
        assertEquals(AgentIntent.COURSE_QUESTION, analyzer.analyze("How do flashcards help with studying?"));
        assertEquals(AgentIntent.FLASHCARD_GENERATION, analyzer.analyze("Create flashcards from my course materials"));
    }

    @Test
    void commonCourseAssistantRequestsMapToExpectedTasks() {
        assertEquals(AgentIntent.QUIZ_GENERATION, analyzer.analyze("Generate a practice quiz"));
        assertEquals(AgentIntent.STUDY_PLAN_CREATION, analyzer.analyze("Create a study plan"));
        assertEquals(AgentIntent.COURSE_SEARCH, analyzer.analyze("Find course materials about threads"));
        assertEquals(AgentIntent.NEXT_STEP, analyzer.analyze("What should I do next?"));
        assertEquals(AgentIntent.PROGRESS_REVIEW, analyzer.analyze("How am I performing?"));
        assertEquals(AgentIntent.COURSE_QUESTION, analyzer.analyze("Explain deadlocks in simple terms"));
    }

    @Test
    void validatorRejectsUnknownTools() {
        var validator = new AgentPlanValidator();
        var plan = new AgentPlan(AgentIntent.PROGRESS_REVIEW,
                java.util.List.of(new AgentPlanStep("SHELL", java.util.Map.of())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(plan, 1L));
    }

    @Test
    void validatorAllowsAllowlistedStudyTools() {
        var validator = new AgentPlanValidator();
        var plan = new AgentPlan(AgentIntent.PROGRESS_REVIEW,
                java.util.List.of(new AgentPlanStep("GENERATE_QUIZ", java.util.Map.of()),
                        new AgentPlanStep("CREATE_STUDY_PLAN", java.util.Map.of()),
                        new AgentPlanStep("ANSWER_COURSE_QUESTION", java.util.Map.of("question", "What is a thread?"))));
        assertDoesNotThrow(() -> validator.validate(plan, 1L));
    }
}
