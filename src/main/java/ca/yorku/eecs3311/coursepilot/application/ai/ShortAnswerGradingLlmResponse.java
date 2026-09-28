package ca.yorku.eecs3311.coursepilot.application.ai;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown=true)
public record ShortAnswerGradingLlmResponse(boolean correct, String feedback, @JsonAlias({"source_ids","sourceIds"}) List<String> sourceIds) { public ShortAnswerGradingLlmResponse { sourceIds=sourceIds==null?List.of():List.copyOf(sourceIds); } }
