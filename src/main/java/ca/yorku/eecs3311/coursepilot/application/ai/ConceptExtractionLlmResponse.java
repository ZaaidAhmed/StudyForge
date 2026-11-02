package ca.yorku.eecs3311.coursepilot.application.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConceptExtractionLlmResponse(@JsonAlias({"key_concepts", "items"}) List<ConceptLlmResponse> concepts) {
    public ConceptExtractionLlmResponse {
        concepts = concepts == null ? List.of() : concepts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConceptLlmResponse(
            @JsonAlias({"concept", "title", "term"}) String name,
            @JsonAlias({"description", "summary"}) String definition,
            String example,
            @JsonAlias({"source_ids", "sourceIDs"}) List<String> sourceIds) {
        public ConceptLlmResponse {
            sourceIds = sourceIds == null ? List.of() : sourceIds;
        }
    }
}
