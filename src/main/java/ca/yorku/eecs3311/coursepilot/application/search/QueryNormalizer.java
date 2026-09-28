package ca.yorku.eecs3311.coursepilot.application.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryNormalizer {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+(?:['’_-][\\p{L}\\p{N}]+)*");

    public NormalizedQuery normalize(String query) {
        String original = query == null ? "" : query.trim();
        ArrayList<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(original.toLowerCase(Locale.ROOT));
        while (matcher.find()) tokens.add(matcher.group());
        var unique = new LinkedHashSet<>(tokens);
        return new NormalizedQuery(original, String.join(" ", tokens), new ArrayList<>(unique));
    }

    public String normalizeText(String text) {
        return String.join(" ", tokenize(text));
    }

    public java.util.List<String> tokenize(String text) {
        ArrayList<String> tokens = new ArrayList<>();
        if (text == null) return tokens;
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) tokens.add(matcher.group());
        return tokens;
    }
}
