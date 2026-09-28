package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import java.util.List;

public record NormalizedText(List<NormalizedPage> pages) {
    public NormalizedText { pages = List.copyOf(pages); }
    public String text() { return pages.stream().map(NormalizedPage::text).reduce((a, b) -> a + "\n\n" + b).orElse(""); }
}
