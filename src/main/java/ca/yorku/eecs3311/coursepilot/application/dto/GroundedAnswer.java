package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record GroundedAnswer(String question, String answer, List<GroundedSource> sources,
                             boolean grounded, boolean insufficientContext) {
    public GroundedAnswer {
        sources = List.copyOf(sources == null ? List.of() : sources);
    }

    public static GroundedAnswer insufficient(String question, String message) {
        return new GroundedAnswer(question, message, List.of(), false, true);
    }
}
