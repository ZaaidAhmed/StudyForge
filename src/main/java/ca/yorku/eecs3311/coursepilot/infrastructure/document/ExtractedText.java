package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import java.util.List;

public record ExtractedText(List<ExtractedPage> pages) {
    public ExtractedText {
        pages = List.copyOf(pages);
    }

    public String text() {
        return pages.stream().map(ExtractedPage::text).filter(value -> value != null)
                .reduce((left, right) -> left + "\n\n" + right).orElse("");
    }

    public Integer pageCount() {
        return pages.stream().map(ExtractedPage::pageNumber).filter(value -> value != null).max(Integer::compareTo).orElse(null);
    }
}
