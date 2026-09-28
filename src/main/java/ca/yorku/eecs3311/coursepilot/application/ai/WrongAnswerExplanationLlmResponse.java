package ca.yorku.eecs3311.coursepilot.application.ai;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown=true)
public record WrongAnswerExplanationLlmResponse(String explanation, @JsonAlias({"source_ids","sourceIds"}) List<String> sourceIds) { public WrongAnswerExplanationLlmResponse { sourceIds=sourceIds==null?List.of():List.copyOf(sourceIds); } }
