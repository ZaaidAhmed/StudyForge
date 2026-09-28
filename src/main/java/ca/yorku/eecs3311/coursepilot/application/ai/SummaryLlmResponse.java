package ca.yorku.eecs3311.coursepilot.application.ai;

import java.util.List;

public record SummaryLlmResponse(String summary, List<String> keyPoints,
                                 List<String> sourceIds, boolean insufficientContext) {
    public SummaryLlmResponse {
        keyPoints = keyPoints == null ? List.of() : keyPoints;
        sourceIds = sourceIds == null ? List.of() : sourceIds;
    }
}
