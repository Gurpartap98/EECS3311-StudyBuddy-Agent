package studybuddy.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Multi-session study plan for a course (F06).
 */
public final class StudyPlan {

    public record Session(LocalDate date, String focusTopic, String task, int minutes) {
        public Session {
            Objects.requireNonNull(date, "date");
            focusTopic = focusTopic == null ? "" : focusTopic;
            task = task == null ? "" : task;
            if (minutes <= 0) {
                throw new IllegalArgumentException("minutes must be positive");
            }
        }
    }

    private final String id;
    private final String courseId;
    private final LocalDate examDate;
    private final double hoursPerDay;
    private final List<Session> sessions;
    private final Instant createdAt;

    public StudyPlan(
            String courseId, LocalDate examDate, double hoursPerDay, List<Session> sessions) {
        this(UUID.randomUUID().toString(), courseId, examDate, hoursPerDay, sessions, Instant.now());
    }

    public StudyPlan(
            String id,
            String courseId,
            LocalDate examDate,
            double hoursPerDay,
            List<Session> sessions,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.examDate = examDate;
        this.hoursPerDay = hoursPerDay;
        this.sessions = sessions == null ? new ArrayList<>() : new ArrayList<>(sessions);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public double getHoursPerDay() {
        return hoursPerDay;
    }

    public List<Session> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
