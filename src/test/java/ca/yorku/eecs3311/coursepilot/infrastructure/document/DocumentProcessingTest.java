package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingTest {
    @Test
    void normalizesLineEndingsWhitespaceAndExcessBlankLinesWithoutLowercasing() {
        var normalizer = new TextNormalizer();
        assertThat(normalizer.normalize("# Heading\r\n\r\n\r\n  Useful   Text\r\nCODE();"))
                .isEqualTo("# Heading\n\n Useful Text\nCODE();");
    }

    @Test
    void chunksDeterministicallyWithBoundedOverlap() {
        var chunker = new TextChunker(30, 5);
        String text = "Paragraph one has useful context.\n\nParagraph two continues the material.\n\nParagraph three finishes it.";

        var first = chunker.chunk(text);
        var second = chunker.chunk(text);

        assertThat(first).containsExactlyElementsOf(second);
        assertThat(first).extracting(ChunkDraft::index).containsExactly(0, 1, 2, 3, 4);
        assertThat(first).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(30));
    }

    @Test
    void rejectsInvalidOverlapConfiguration() {
        assertThatThrownBy(() -> new TextChunker(100, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsUtf8TextAndPreservesUnicode() {
        var extractor = new TextDocumentExtractor();
        var extracted = extractor.extract("Résumé — стратегия\n第二行".getBytes(StandardCharsets.UTF_8));
        assertThat(extracted.text()).contains("Résumé", "стратегия", "第二行");
        assertThat(extracted.pageCount()).isNull();
    }

    @Test
    void validatesSupportedTypesAndRejectsUnsafeOrEmptyFiles() {
        var validator = new DocumentValidator(10);
        assertThat(validator.validate("LECTURE.PDF", new byte[]{1})).isEqualTo("pdf");
        assertThatThrownBy(() -> validator.validate("notes.exe", new byte[]{1}))
                .hasMessageContaining("Unsupported file type");
        assertThatThrownBy(() -> validator.validate("../notes.txt", new byte[]{1}))
                .hasMessageContaining("filename");
        assertThatThrownBy(() -> validator.validate("notes.txt", new byte[0]))
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> validator.validate("notes.txt", new byte[11]))
                .hasMessageContaining("10 MB");
    }
}
