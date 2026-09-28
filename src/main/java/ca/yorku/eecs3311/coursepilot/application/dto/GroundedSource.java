package ca.yorku.eecs3311.coursepilot.application.dto;

public record GroundedSource(String sourceId, Long documentId, String documentName, Long chunkId,
                             int chunkIndex, Integer pageStart, Integer pageEnd, String snippet) { }
