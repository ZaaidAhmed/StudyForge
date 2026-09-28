package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record SummaryResult(String summaryType, String label, String summary,
                            List<String> keyPoints, List<GroundedSource> sources,
                            boolean grounded, boolean insufficientContext) {
    public SummaryResult {
        keyPoints = keyPoints == null ? List.of() : List.copyOf(keyPoints);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
