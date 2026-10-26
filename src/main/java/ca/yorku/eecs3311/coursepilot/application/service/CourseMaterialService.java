package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ImportResult;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseDocumentRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.ChunkDraft;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentExtractorFactory;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentImportException;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentValidator;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.FileStorageService;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.HashService;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.NormalizedText;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.StoredFile;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.TextChunker;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Service
@Transactional
public class CourseMaterialService {
    private final CourseRepository courseRepository;
    private final CourseDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentValidator validator;
    private final HashService hashService;
    private final FileStorageService storage;
    private final DocumentExtractorFactory extractorFactory;
    private final TextNormalizer normalizer;
    private final TextChunker chunker;

    public CourseMaterialService(CourseRepository courseRepository, CourseDocumentRepository documentRepository,
                                 DocumentChunkRepository chunkRepository, DocumentValidator validator,
                                 HashService hashService, FileStorageService storage,
                                 DocumentExtractorFactory extractorFactory, TextNormalizer normalizer,
                                 TextChunker chunker) {
        this.courseRepository = courseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.validator = validator;
        this.hashService = hashService;
        this.storage = storage;
        this.extractorFactory = extractorFactory;
        this.normalizer = normalizer;
        this.chunker = chunker;
    }

    public ImportResult importMaterial(Long courseId, String filename, String contentType, byte[] content) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course with id '" + courseId + "' was not found."));
        String extension = validator.validate(filename, contentType, content);
        String hash = hashService.sha256(content);
        if (documentRepository.findByCourseIdAndSha256Hash(courseId, hash).isPresent()) {
            throw new DuplicateDocumentException("This material has already been uploaded to this course.");
        }

        StoredFile stored = null;
        try {
            stored = storage.store(extension, content);
            var extracted = extractorFactory.forExtension(extension).extract(content);
            NormalizedText normalized = normalizer.normalize(extracted);
            if (normalized.text().isBlank()) {
                throw new DocumentImportException("No extractable text was found in this document.");
            }
            List<ChunkDraft> drafts = chunker.chunk(normalized);
            if (drafts.isEmpty()) throw new DocumentImportException("No extractable text was found in this document.");
            CourseDocument document = documentRepository.saveAndFlush(new CourseDocument(course, filename,
                    stored.filename(), contentType, extension, hash, normalized.text().length(), drafts.size(), extracted.pageCount()));
            chunkRepository.saveAll(drafts.stream().map(draft -> new DocumentChunk(document, draft.index(), draft.content(), draft.startPage(), draft.endPage())).toList());
            return new ImportResult(DocumentResponse.from(document), drafts.size(), normalized.text().length());
        } catch (DuplicateDocumentException | CourseNotFoundException exception) {
            storage.deleteIfPresent(stored);
            throw exception;
        } catch (RuntimeException exception) {
            storage.deleteIfPresent(stored);
            if (exception instanceof DocumentImportException) throw exception;
            throw new DocumentImportException("The material could not be processed.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long courseId) {
        if (!courseRepository.existsById(courseId)) throw new CourseNotFoundException("Course with id '" + courseId + "' was not found.");
        return documentRepository.findByCourseIdOrderByUploadedAtDesc(courseId).stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long courseId, Long documentId) {
        CourseDocument document = documentRepository.findById(documentId)
                .filter(value -> value.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new DocumentNotFoundException("Material with id '" + documentId + "' was not found for this course."));
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> listChunks(Long courseId, Long documentId) {
        getDocument(courseId, documentId);
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long courseId, Long documentId) {
        CourseDocument document = documentRepository.findById(documentId)
                .filter(value -> value.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new DocumentNotFoundException("Material with id '" + documentId + "' was not found for this course."));
        return new FileSystemResource(storage.load(document.getStoredFilename()));
    }
}
