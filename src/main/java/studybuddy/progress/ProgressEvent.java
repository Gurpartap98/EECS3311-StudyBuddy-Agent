package studybuddy.progress;

import java.time.Instant;
import java.util.Objects;

/** Immutable progress notification payload. */
public final class ProgressEvent {

    private final String courseId;
    private final String topic;
    private final double scorePercent;
    private final Instant at;

    public ProgressEvent(String courseId, String topic, double scorePercent) {
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.topic = topic == null ? "" : topic;
        this.scorePercent = scorePercent;
        this.at = Instant.now();
    }

    public String getCourseId() {
        return courseId;
    }

    public String getTopic() {
        return topic;
    }

    public double getScorePercent() {
        return scorePercent;
    }

    public Instant getAt() {
        return at;
    }
}
