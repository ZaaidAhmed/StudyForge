package ca.yorku.eecs3311.coursepilot.application.dto;

public record FlashcardSourceResponse(Long documentId, String documentName, Long chunkId, int chunkIndex, Integer pageStart, Integer pageEnd) { }
