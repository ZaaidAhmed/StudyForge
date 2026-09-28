package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private CourseDocument document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, length = 1500)
    private String content;

    @Column(name = "character_count", nullable = false)
    private int characterCount;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;

    protected DocumentChunk() { }

    public DocumentChunk(CourseDocument document, int chunkIndex, String content, Integer startPage, Integer endPage) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.characterCount = content.length();
        this.startPage = startPage;
        this.endPage = endPage;
    }

    public Long getId() { return id; }
    public CourseDocument getDocument() { return document; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public int getCharacterCount() { return characterCount; }
    public Integer getStartPage() { return startPage; }
    public Integer getEndPage() { return endPage; }
}
