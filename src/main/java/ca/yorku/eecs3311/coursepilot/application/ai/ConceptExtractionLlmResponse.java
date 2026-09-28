package ca.yorku.eecs3311.coursepilot.application.ai;

import java.util.List;

public record ConceptExtractionLlmResponse(List<ConceptLlmResponse> concepts) {
    public ConceptExtractionLlmResponse {
        concepts = concepts == null ? List.of() : concepts;
    }

    public record ConceptLlmResponse(String name, String definition, List<String> sourceIds) {
        public ConceptLlmResponse {
            sourceIds = sourceIds == null ? List.of() : sourceIds;
        }
    }
}
