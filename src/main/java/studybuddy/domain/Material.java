package studybuddy.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Uploaded learning material with extracted text (F02).
 */
public final class Material {

    private final String id;
    private final String courseId;
    private String title;
    private final String sourcePath;
    private String extractedText;
    private final List<String> tags;
    private String summary;
    private final Instant createdAt;

    public Material(String courseId, String title, String sourcePath, String extractedText) {
        this(
                UUID.randomUUID().toString(),
                courseId,
                title,
                sourcePath,
                extractedText,
                new ArrayList<>(),
                null,
                Instant.now());
    }

    public Material(
            String id,
            String courseId,
            String title,
            String sourcePath,
            String extractedText,
            List<String> tags,
            String summary,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.title = requireNonBlank(title, "title");
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.extractedText = extractedText == null ? "" : extractedText;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.summary = summary;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = requireNonBlank(title, "title");
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText == null ? "" : extractedText;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isBlank() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "Material{id='%s', courseId='%s', title='%s'}"
                .formatted(id, courseId, title);
    }
}
