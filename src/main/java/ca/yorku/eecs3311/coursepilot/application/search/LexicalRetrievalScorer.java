package ca.yorku.eecs3311.coursepilot.application.search;

import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LexicalRetrievalScorer implements RetrievalScorer {
    private final QueryNormalizer normalizer;
    private final double exactPhraseWeight;
    private final double termCoverageWeight;
    private final double termWeight;
    private final double termFrequencyWeight;
    private final double titleWeight;

    public LexicalRetrievalScorer(QueryNormalizer normalizer,
                                  @Value("${coursepilot.retrieval.exact-phrase-weight:6.0}") double exactPhraseWeight,
                                  @Value("${coursepilot.retrieval.term-coverage-weight:4.0}") double termCoverageWeight,
                                  @Value("${coursepilot.retrieval.term-weight:1.5}") double termWeight,
                                  @Value("${coursepilot.retrieval.term-frequency-weight:0.25}") double termFrequencyWeight,
                                  @Value("${coursepilot.retrieval.title-weight:2.0}") double titleWeight) {
        this.normalizer = normalizer;
        this.exactPhraseWeight = exactPhraseWeight;
        this.termCoverageWeight = termCoverageWeight;
        this.termWeight = termWeight;
        this.termFrequencyWeight = termFrequencyWeight;
        this.titleWeight = titleWeight;
    }

    @Override
    public ScoredChunk score(NormalizedQuery query, DocumentChunk chunk) {
        String content = normalizer.normalizeText(chunk.getContent());
        String title = normalizer.normalizeText(chunk.getDocument().getOriginalFilename());
        Map<String, Integer> frequencies = frequencies(normalizer.tokenize(chunk.getContent()));
        List<String> matched = new ArrayList<>(query.terms().stream().filter(frequencies::containsKey).toList());
        if (query.terms().isEmpty() || matched.isEmpty()) return new ScoredChunk(chunk, 0.0, matched);

        double score = 0.0;
        if (!query.phrase().isBlank() && content.contains(query.phrase())) score += exactPhraseWeight;
        double coverage = (double) matched.size() / query.terms().size();
        score += coverage * termCoverageWeight;
        score += matched.size() * termWeight;
        score += matched.stream().mapToDouble(term -> Math.min(3, frequencies.get(term)) * termFrequencyWeight).sum();
        if (query.terms().stream().allMatch(title::contains)) score += titleWeight;
        return new ScoredChunk(chunk, score, matched);
    }

    private Map<String, Integer> frequencies(List<String> tokens) {
        Map<String, Integer> result = new HashMap<>();
        for (String token : tokens) result.merge(token, 1, Integer::sum);
        return result;
    }
}
