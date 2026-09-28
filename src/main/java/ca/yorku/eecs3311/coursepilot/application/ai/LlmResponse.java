package ca.yorku.eecs3311.coursepilot.application.ai;

public record LlmResponse(String content, String model, long durationMs) { }
