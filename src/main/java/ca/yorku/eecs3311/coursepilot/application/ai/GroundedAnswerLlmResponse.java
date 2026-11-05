package ca.yorku.eecs3311.coursepilot.application.ai;

import java.util.List;

public record GroundedAnswerLlmResponse(String answer, List<String> sourceIds, boolean insufficientContext,
                                        List<String> keyConcepts) {
    public GroundedAnswerLlmResponse {
        sourceIds = List.copyOf(sourceIds == null ? List.of() : sourceIds);
        answer = answer == null ? "" : answer.trim();
        keyConcepts = List.copyOf(keyConcepts == null ? List.of() : keyConcepts);
    }

    public GroundedAnswerLlmResponse(String answer, List<String> sourceIds, boolean insufficientContext) {
        this(answer, sourceIds, insufficientContext, List.of());
    }
}
