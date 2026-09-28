package ca.yorku.eecs3311.coursepilot.application.search;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalRetrievalScorerTest {
    private final QueryNormalizer normalizer = new QueryNormalizer();
    private final LexicalRetrievalScorer scorer = new LexicalRetrievalScorer(normalizer, 6.0, 4.0, 1.5, 0.25, 2.0);

    @Test
    void exactPhraseAndCoverageOutrankPartialMatches() {
        var exact = chunk("Lecture 06 - Design Patterns.pdf", "The Strategy pattern defines a family of algorithms.", 1);
        var partial = chunk("Week 06 Notes.pdf", "This lecture discusses software patterns and development strategies.", 2);

        var exactScore = scorer.score(normalizer.normalize("strategy pattern"), exact);
        var partialScore = scorer.score(normalizer.normalize("strategy pattern"), partial);

        assertThat(exactScore.score()).isGreaterThan(partialScore.score());
        assertThat(exactScore.matchedTerms()).containsExactly("strategy", "pattern");
    }

    @Test
    void matchingIsCaseInsensitiveAndPunctuationAware() {
        var chunk = chunk("notes.md", "OBSERVER, pattern: event-notification.", 1);
        var result = scorer.score(normalizer.normalize("observer pattern"), chunk);
        assertThat(result.score()).isGreaterThan(0);
        assertThat(result.matchedTerms()).containsExactly("observer", "pattern");
    }

    @Test
    void repeatedTermsHaveABoundedContribution() {
        var normal = scorer.score(normalizer.normalize("strategy"), chunk("notes.md", "strategy", 1));
        var repeated = scorer.score(normalizer.normalize("strategy"), chunk("notes.md", "strategy ".repeat(50), 2));
        assertThat(repeated.score() - normal.score()).isLessThan(1.0);
    }

    private DocumentChunk chunk(String filename, String content, int index) {
        Course course = new Course("EECS3311", "Software Design", null, null);
        CourseDocument document = new CourseDocument(course, filename, "stored.pdf", "text/plain", "txt", "hash" + index, content.length(), 1, null);
        ReflectionTestUtils.setField(document, "id", (long) index);
        DocumentChunk chunk = new DocumentChunk(document, index, content, null, null);
        ReflectionTestUtils.setField(chunk, "id", (long) index);
        return chunk;
    }
}
