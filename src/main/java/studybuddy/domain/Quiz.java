package studybuddy.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Generated quiz containing typed Question products (F08).
 */
public final class Quiz {

    private final String id;
    private final String courseId;
    private final String title;
    private final List<Question> questions;
    private final Instant createdAt;

    public Quiz(String courseId, String title, List<Question> questions) {
        this(UUID.randomUUID().toString(), courseId, title, questions, Instant.now());
    }

    public Quiz(
            String id,
            String courseId,
            String title,
            List<Question> questions,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.title = title == null || title.isBlank() ? "Quiz" : title.trim();
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
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

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int size() {
        return questions.size();
    }
}
