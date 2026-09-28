package ca.yorku.eecs3311.coursepilot.infrastructure.document;

public record ChunkDraft(int index, String content, Integer startPage, Integer endPage) { }
