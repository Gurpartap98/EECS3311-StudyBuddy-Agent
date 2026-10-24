package studybuddy.domain;

import java.util.Locale;

/**
 * Concrete short-answer question product.
 */
public final class ShortAnswerQuestion extends Question {

    private final String expectedAnswer;

    public ShortAnswerQuestion(String prompt, String topic, String difficulty, String expectedAnswer) {
        this(null, prompt, topic, difficulty, expectedAnswer);
    }

    public ShortAnswerQuestion(
            String id,
            String prompt,
            String topic,
            String difficulty,
            String expectedAnswer) {
        super(id == null ? java.util.UUID.randomUUID().toString() : id, prompt, topic, difficulty);
        if (expectedAnswer == null || expectedAnswer.isBlank()) {
            throw new IllegalArgumentException("expectedAnswer must not be blank");
        }
        this.expectedAnswer = expectedAnswer.trim();
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    /** Case-insensitive trim compare for deterministic grading. */
    public boolean matches(String studentAnswer) {
        if (studentAnswer == null) {
            return false;
        }
        return expectedAnswer
                .toLowerCase(Locale.ROOT)
                .equals(studentAnswer.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public String getType() {
        return "SHORT_ANSWER";
    }

    @Override
    public String getCorrectAnswer() {
        return expectedAnswer;
    }
}
