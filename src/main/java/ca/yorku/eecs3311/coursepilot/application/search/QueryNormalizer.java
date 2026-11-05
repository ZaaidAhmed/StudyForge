package ca.yorku.eecs3311.coursepilot.application.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryNormalizer {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+(?:['_-][\\p{L}\\p{N}]+)*");
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "as", "at", "be", "between",
            "can", "could", "describe", "define", "did", "do", "does", "explain", "for", "from", "how",
            "i", "in", "into", "is", "it", "me", "my", "of", "on", "or", "please", "should", "tell",
            "that", "the", "these", "this", "those", "to", "was", "what", "when", "where", "which", "who",
            "why", "with", "would", "your", "s");

    public NormalizedQuery normalize(String query) {
        String original = query == null ? "" : query.trim();
        List<String> tokens = tokenize(original);
        var unique = new LinkedHashSet<>(tokens.stream().filter(token -> !STOP_WORDS.contains(token)).toList());
        return new NormalizedQuery(original, String.join(" ", tokens), new ArrayList<>(unique));
    }

    public String normalizeText(String text) {
        return String.join(" ", tokenize(text));
    }

    public List<String> tokenize(String text) {
        ArrayList<String> tokens = new ArrayList<>();
        if (text == null) return tokens;
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) tokens.add(normalizeTerm(matcher.group()));
        return tokens;
    }

    private String normalizeTerm(String term) {
        if (term.length() > 5 && term.endsWith("ies")) return term.substring(0, term.length() - 3) + "y";
        if (term.length() > 5 && term.endsWith("es")
                && (term.endsWith("ses") || term.endsWith("xes") || term.endsWith("zes") || term.endsWith("ches") || term.endsWith("shes")))
            return term.substring(0, term.length() - 2);
        if (term.length() > 4 && term.endsWith("s") && !term.endsWith("ss")) return term.substring(0, term.length() - 1);
        return term;
    }
}
