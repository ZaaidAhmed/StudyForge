package ca.yorku.eecs3311.coursepilot.application.dto;
import java.util.List;
public record GroundedExplanation(String explanation, List<GroundedSource> sources, boolean unavailable) { public GroundedExplanation { sources=List.copyOf(sources); } }
