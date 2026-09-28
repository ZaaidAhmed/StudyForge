package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${coursepilot.storage.upload-dir:./data/uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(String extension, byte[] content) {
        try {
            Files.createDirectories(root);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) throw new DocumentImportException("The upload path is unsafe.");
            Files.write(target, content);
            return new StoredFile(filename, target);
        } catch (IOException exception) {
            throw new DocumentImportException("The uploaded file could not be stored.", exception);
        }
    }

    public void deleteIfPresent(StoredFile storedFile) {
        if (storedFile == null) return;
        try {
            Path target = storedFile.path().toAbsolutePath().normalize();
            if (target.startsWith(root)) Files.deleteIfExists(target);
        } catch (IOException exception) {
            // Cleanup is best effort; the original import error remains the useful user-facing result.
        }
    }

    public Path load(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) throw new DocumentImportException("The stored material file was not found.");
        return target;
    }
}
