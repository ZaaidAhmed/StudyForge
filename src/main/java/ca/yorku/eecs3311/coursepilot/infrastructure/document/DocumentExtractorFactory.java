package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentExtractorFactory {
    private final List<DocumentTextExtractor> extractors;

    public DocumentExtractorFactory(List<DocumentTextExtractor> extractors) { this.extractors = extractors; }

    public DocumentTextExtractor forExtension(String extension) {
        return extractors.stream().filter(extractor -> extractor.supports(extension)).findFirst()
                .orElseThrow(() -> new DocumentImportException("No extractor is available for this file type."));
    }
}
