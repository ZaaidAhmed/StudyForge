package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Prepares readable study material without changing the searchable source chunks. */
public final class FlashcardMaterialPreprocessor {
    private static final String PROFESSOR_LINE = "(?i).*\\b(?:dr\\.?\\s+)?r\\.?\\s*al\\s*omari\\b.*";
    private static final String PAGE_LINE = "(?i)^\\s*(?:page\\s+)?\\d{1,4}(?:\\s+of\\s+\\d{1,4})?\\s*$";
    private static final String TIMESTAMP_LINE = "^\\s*(?:\\d{1,2}:){1,2}\\d{1,2}\\s*$";

    public List<SearchResult> clean(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();
        Map<String, Integer> lineCounts = lineCounts(chunks);
        List<SearchResult> cleaned = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String text = cleanChunk(chunk.getContent(), lineCounts);
            if (text.length() < 40 || sentenceCount(text) == 0) continue;
            cleaned.add(new SearchResult(chunk.getDocument().getId(), chunk.getId(),
                    chunk.getDocument().getOriginalFilename(), chunk.getChunkIndex(), text, 1d,
                    chunk.getStartPage(), chunk.getEndPage(), List.of()));
        }
        return List.copyOf(cleaned);
    }

    private String cleanChunk(String value, Map<String, Integer> lineCounts) {
        String text = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .replace("\u00a0", " ")
                .replace("Â©", "")
                .replace("â€¦", "...")
                .replace("â€“", "-")
                .replace("â€”", "-")
                .replace("â€œ", "\"")
                .replace("â€", "\"")
                .replace("â€™", "'")
                .replaceAll("(?m)(?<=\\w)-\\s*\\n\\s*(?=\\w)", "")
                .replace("\r\n", "\n").replace('\r', '\n');
        List<String> kept = new ArrayList<>();
        for (String raw : text.split("\\n")) {
            String line = raw.replaceAll("[ \\t]+", " ").trim()
                    .replaceAll("(?i)(?<![A-Za-z])[#@|]{2,}(?:\\s+\\d+)?", "")
                    .replaceAll("\\s{2,}", " ").trim();
            if (line.isBlank() || line.matches(PAGE_LINE) || line.matches(TIMESTAMP_LINE)
                    || line.matches(PROFESSOR_LINE) || isRepeatedNoise(line, lineCounts)) continue;
            if (looksLikeOcrArtifact(line)) continue;
            kept.add(line);
        }
        return String.join(" ", kept).replaceAll("\\s{2,}", " ").trim();
    }

    private boolean isRepeatedNoise(String line, Map<String, Integer> counts) {
        return line.length() <= 120 && counts.getOrDefault(line.toLowerCase(Locale.ROOT), 0) >= 2
                && !looksLikeHeading(line);
    }

    private boolean looksLikeHeading(String line) {
        return line.length() <= 90 && (line.matches("[A-Z0-9][A-Z0-9 \\-/&:()]{4,}")
                || line.matches("(?i)^(chapter|unit|lecture|topic|section)\\b.*"));
    }

    private boolean looksLikeOcrArtifact(String line) {
        long letters = line.chars().filter(Character::isLetter).count();
        long symbols = line.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        return line.length() >= 8 && letters <= 2 && symbols > letters;
    }

    private int sentenceCount(String text) {
        return text.split("(?<=[.!?])\\s+").length;
    }

    private Map<String, Integer> lineCounts(List<DocumentChunk> chunks) {
        Map<String, Integer> counts = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            Set<String> seenInChunk = new HashSet<>();
            for (String raw : chunk.getContent().split("\\R")) {
                String line = raw.replaceAll("[ \\t]+", " ").trim().toLowerCase(Locale.ROOT);
                if (line.length() >= 8 && line.length() <= 120 && seenInChunk.add(line))
                    counts.merge(line, 1, Integer::sum);
            }
        }
        return counts;
    }
}
