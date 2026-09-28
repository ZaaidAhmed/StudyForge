package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcard_sources")
public class FlashcardSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flashcard_id", nullable = false) private Flashcard flashcard;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false) private CourseDocument document;
    @Column(name = "chunk_id", nullable = false) private Long chunkId;
    @Column(name = "chunk_index", nullable = false) private int chunkIndex;
    private Integer pageStart; private Integer pageEnd;
    protected FlashcardSource() { }
    public FlashcardSource(Flashcard flashcard, CourseDocument document, Long chunkId, int chunkIndex, Integer pageStart, Integer pageEnd) {
        this.flashcard = flashcard; this.document = document; this.chunkId = chunkId; this.chunkIndex = chunkIndex; this.pageStart = pageStart; this.pageEnd = pageEnd;
    }
    public Long getId() { return id; } public Flashcard getFlashcard() { return flashcard; }
    public CourseDocument getDocument() { return document; } public Long getChunkId() { return chunkId; }
    public int getChunkIndex() { return chunkIndex; } public Integer getPageStart() { return pageStart; } public Integer getPageEnd() { return pageEnd; }
}
