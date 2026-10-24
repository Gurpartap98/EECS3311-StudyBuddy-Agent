package studybuddy.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Specification used by QuizFactory.createQuestion(spec).
 */
public final class QuestionSpec {

    public enum Kind {
        MCQ,
        SHORT_ANSWER
    }

    private final Kind kind;
    private final String prompt;
    private final String topic;
    private final String difficulty;
    private final List<String> choices;
    private final int correctIndex;
    private final String expectedAnswer;

    private QuestionSpec(
            Kind kind,
            String prompt,
            String topic,
            String difficulty,
            List<String> choices,
            int correctIndex,
            String expectedAnswer) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.topic = topic == null ? "" : topic;
        this.difficulty = difficulty == null ? "medium" : difficulty;
        this.choices = choices == null ? List.of() : List.copyOf(choices);
        this.correctIndex = correctIndex;
        this.expectedAnswer = expectedAnswer;
    }

    public static QuestionSpec mcq(
            String prompt,
            String topic,
            String difficulty,
            List<String> choices,
            int correctIndex) {
        return new QuestionSpec(
                Kind.MCQ, prompt, topic, difficulty, new ArrayList<>(choices), correctIndex, null);
    }

    public static QuestionSpec shortAnswer(
            String prompt, String topic, String difficulty, String expectedAnswer) {
        return new QuestionSpec(
                Kind.SHORT_ANSWER, prompt, topic, difficulty, List.of(), -1, expectedAnswer);
    }

    public Kind getKind() {
        return kind;
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

    public List<String> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }
}
