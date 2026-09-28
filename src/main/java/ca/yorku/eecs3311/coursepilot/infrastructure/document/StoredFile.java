package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import java.nio.file.Path;

public record StoredFile(String filename, Path path) { }
