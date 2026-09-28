package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TextDocumentExtractor implements DocumentTextExtractor {
    @Override public boolean supports(String extension) { return extension.equals("txt") || extension.equals("md"); }
    @Override public ExtractedText extract(byte[] content) {
        return new ExtractedText(List.of(new ExtractedPage(null, new String(content, StandardCharsets.UTF_8))));
    }
}
