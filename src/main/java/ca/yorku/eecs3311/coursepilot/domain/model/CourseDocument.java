package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "course_documents")
public class CourseDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 120)
    private String storedFilename;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProcessingStatus processingStatus;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "extracted_character_count", nullable = false)
    private int extractedCharacterCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected CourseDocument() { }

    public CourseDocument(Course course, String originalFilename, String storedFilename, String contentType,
                          String fileType, String sha256Hash, int extractedCharacterCount, int chunkCount,
                          Integer pageCount) {
        this.course = course;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileType = fileType;
        this.sha256Hash = sha256Hash;
        this.processingStatus = ProcessingStatus.READY;
        this.extractedCharacterCount = extractedCharacterCount;
        this.chunkCount = chunkCount;
        this.pageCount = pageCount;
    }

    @PrePersist
    void onCreate() { uploadedAt = Instant.now(); }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getContentType() { return contentType; }
    public String getFileType() { return fileType; }
    public String getSha256Hash() { return sha256Hash; }
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public Instant getUploadedAt() { return uploadedAt; }
    public int getExtractedCharacterCount() { return extractedCharacterCount; }
    public int getChunkCount() { return chunkCount; }
    public Integer getPageCount() { return pageCount; }
    public String getErrorMessage() { return errorMessage; }
}
