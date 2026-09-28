package ca.yorku.eecs3311.coursepilot.application.dto;

public record ImportResult(DocumentResponse document, int chunkCount, int extractedCharacterCount) { }
