package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TextNormalizer {
    public String normalize(String value) {
        return normalizePage(value == null ? "" : value);
    }

    public NormalizedText normalize(ExtractedText extracted) {
        List<NormalizedPage> pages = extracted.pages().stream()
                .map(page -> new NormalizedPage(page.pageNumber(), normalizePage(page.text())))
                .filter(page -> !page.text().isBlank()).toList();
        return new NormalizedText(pages);
    }

    private String normalizePage(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n')
                .lines().map(String::stripTrailing)
                .reduce((left, right) -> left + "\n" + right).orElse("")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
