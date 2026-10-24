package studybuddy.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Abstract quiz question product (Factory Method product hierarchy).
 */
public abstract class Question {

    private final String id;
    private final String prompt;
    private final String topic;
    private final String difficulty;

    protected Question(String prompt, String topic, String difficulty) {
        this(UUID.randomUUID().toString(), prompt, topic, difficulty);
    }

    protected Question(String id, String prompt, String topic, String difficulty) {
        this.id = Objects.requireNonNull(id, "id");
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        this.prompt = prompt.trim();
        this.topic = topic == null ? "" : topic.trim();
        this.difficulty = difficulty == null ? "medium" : difficulty.trim();
    }

    public String getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    /** Returns a short type label such as {@code MCQ} or {@code SHORT_ANSWER}. */
    public abstract String getType();

    /** Correct answer text used by grading and explanations. */
    public abstract String getCorrectAnswer();
}
