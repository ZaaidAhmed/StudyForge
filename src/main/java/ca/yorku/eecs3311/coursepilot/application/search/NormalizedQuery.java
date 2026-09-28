package ca.yorku.eecs3311.coursepilot.application.search;

import java.util.List;

public record NormalizedQuery(String original, String phrase, List<String> terms) {
    public NormalizedQuery {
        terms = List.copyOf(terms);
    }
}
