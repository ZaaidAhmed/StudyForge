package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
    private final int targetSize;
    private final int overlap;

    public TextChunker(@Value("${coursepilot.retrieval.chunk-size:1200}") int targetSize,
                       @Value("${coursepilot.retrieval.chunk-overlap:150}") int overlap) {
        if (targetSize <= 0 || overlap < 0 || overlap >= targetSize) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative and smaller than chunk size.");
        }
        this.targetSize = targetSize;
        this.overlap = overlap;
    }

    public List<ChunkDraft> chunk(String text) {
        return chunk(new NormalizedText(List.of(new NormalizedPage(null, text == null ? "" : text))));
    }

    public List<ChunkDraft> chunk(NormalizedText document) {
        String text = document.text();
        if (text.isBlank()) return List.of();
        List<ChunkDraft> result = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + targetSize, text.length());
            if (end < text.length()) end = preferredBoundary(text, start, end);
            String content = text.substring(start, end).strip();
            if (!content.isEmpty()) {
                result.add(new ChunkDraft(index++, content, pageAt(document, start), pageAt(document, Math.max(start, end - 1))));
            }
            if (end >= text.length()) break;
            int next = Math.max(start + 1, end - overlap);
            start = next;
        }
        return result;
    }

    private int preferredBoundary(String text, int start, int end) {
        int paragraph = text.lastIndexOf("\n\n", end);
        if (paragraph > start + targetSize / 2) return paragraph + 2;
        int sentence = Math.max(Math.max(text.lastIndexOf('.', end), text.lastIndexOf('?', end)), text.lastIndexOf('!', end));
        if (sentence > start + targetSize / 2) return sentence + 1;
        int whitespace = text.lastIndexOf(' ', end);
        return whitespace > start ? whitespace : end;
    }

    private Integer pageAt(NormalizedText document, int offset) {
        int cursor = 0;
        for (NormalizedPage page : document.pages()) {
            int end = cursor + page.text().length();
            if (offset <= end) return page.pageNumber();
            cursor = end + 2;
        }
        return null;
    }
}
