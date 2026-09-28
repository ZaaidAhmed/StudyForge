package ca.yorku.eecs3311.coursepilot.application.dto;

import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;

import java.time.Instant;

public record DocumentResponse(Long id, String originalFilename, String fileType, String contentType,
                               String sha256Hash, String processingStatus, Instant uploadedAt,
                               int extractedCharacterCount, int chunkCount, Integer pageCount) {
    public static DocumentResponse from(CourseDocument document) {
        return new DocumentResponse(document.getId(), document.getOriginalFilename(), document.getFileType(),
                document.getContentType(), document.getSha256Hash(), document.getProcessingStatus().name(),
                document.getUploadedAt(), document.getExtractedCharacterCount(), document.getChunkCount(), document.getPageCount());
    }
}
