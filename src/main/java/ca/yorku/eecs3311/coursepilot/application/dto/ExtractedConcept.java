package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record ExtractedConcept(String name, String definition, String example, List<GroundedSource> sources) {
    public ExtractedConcept {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
