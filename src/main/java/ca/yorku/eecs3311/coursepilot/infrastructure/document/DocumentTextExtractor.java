package ca.yorku.eecs3311.coursepilot.infrastructure.document;

public interface DocumentTextExtractor {
    boolean supports(String extension);
    ExtractedText extract(byte[] content);
}
