package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentValidator {
    private static final Set<String> SUPPORTED = Set.of("pdf", "txt", "md");
    private final long maxFileSize;

    public DocumentValidator(@Value("${coursepilot.storage.max-file-size:10485760}") long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String validate(String filename, byte[] content) {
        if (filename == null || filename.isBlank()) throw new DocumentValidationException("A filename is required.");
        if (content == null || content.length == 0) throw new DocumentValidationException("This file is empty.");
        if (content.length > maxFileSize) throw new DocumentValidationException("This file exceeds the configured 10 MB limit.");
        String cleanName = Paths.get(filename).getFileName().toString();
        if (!cleanName.equals(filename) || cleanName.contains("..")) {
            throw new DocumentValidationException("The filename is invalid.");
        }
        int dot = cleanName.lastIndexOf('.');
        if (dot <= 0 || dot == cleanName.length() - 1) {
            throw new DocumentValidationException("Unsupported file type. Upload PDF, TXT, or Markdown.");
        }
        String extension = cleanName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(extension)) {
            throw new DocumentValidationException("Unsupported file type. Upload PDF, TXT, or Markdown.");
        }
        return extension;
    }
}
