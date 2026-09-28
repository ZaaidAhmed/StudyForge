package ca.yorku.eecs3311.coursepilot.application.ai;

import java.util.List;

public record GroundedAnswerLlmResponse(String answer, List<String> sourceIds, boolean insufficientContext) {
    public GroundedAnswerLlmResponse {
        sourceIds = List.copyOf(sourceIds == null ? List.of() : sourceIds);
        answer = answer == null ? "" : answer.trim();
    }
}
