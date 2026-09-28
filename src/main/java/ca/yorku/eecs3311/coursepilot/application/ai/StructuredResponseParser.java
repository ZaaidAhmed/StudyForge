package ca.yorku.eecs3311.coursepilot.application.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class StructuredResponseParser {
    private final ObjectMapper objectMapper;

    public StructuredResponseParser(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public <T> T parse(String content, Class<T> type) {
        String json = stripFence(content);
        try { return read(json, () -> objectMapper.readValue(json, type)); }
        catch (StructuredOutputParseException first) {
            String extracted = extractObject(json);
            if (!extracted.equals(json)) return read(extracted, () -> objectMapper.readValue(extracted, type));
            throw first;
        }
    }

    public <T> T parse(String content, TypeReference<T> type) {
        return read(stripFence(content), () -> objectMapper.readValue(stripFence(content), type));
    }

    private <T> T read(String json, Reader<T> reader) {
        if (json.isBlank()) throw new StructuredOutputParseException("The AI response was not valid JSON.", null);
        try { return reader.read(); }
        catch (Exception exception) {
            throw new StructuredOutputParseException("The AI response was not valid JSON.", exception);
        }
    }

    private String stripFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline > 0 && lastFence > firstNewline) return trimmed.substring(firstNewline + 1, lastFence).trim();
        return trimmed;
    }

    private String extractObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start >= 0 && end > start ? content.substring(start, end + 1).trim() : content;
    }

    @FunctionalInterface private interface Reader<T> { T read() throws Exception; }
}
