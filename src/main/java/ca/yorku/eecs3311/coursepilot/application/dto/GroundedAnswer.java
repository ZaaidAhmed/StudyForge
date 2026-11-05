package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record GroundedAnswer(String question, String answer, List<GroundedSource> sources,
                             boolean grounded, boolean insufficientContext, List<String> keyConcepts) {
    public GroundedAnswer {
        sources = List.copyOf(sources == null ? List.of() : sources);
        keyConcepts = List.copyOf(keyConcepts == null ? List.of() : keyConcepts);
    }

    public GroundedAnswer(String question, String answer, List<GroundedSource> sources,
                          boolean grounded, boolean insufficientContext) {
        this(question, answer, sources, grounded, insufficientContext, List.of());
    }

    public static GroundedAnswer insufficient(String question, String message) {
        return new GroundedAnswer(question, message, List.of(), false, true, List.of());
    }
}
