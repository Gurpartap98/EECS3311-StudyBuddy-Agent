package studybuddy.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Concrete multiple-choice question product.
 */
public final class MCQQuestion extends Question {

    private final List<String> choices;
    private final int correctIndex;

    public MCQQuestion(
            String prompt,
            String topic,
            String difficulty,
            List<String> choices,
            int correctIndex) {
        this(null, prompt, topic, difficulty, choices, correctIndex);
    }

    public MCQQuestion(
            String id,
            String prompt,
            String topic,
            String difficulty,
            List<String> choices,
            int correctIndex) {
        super(id == null ? java.util.UUID.randomUUID().toString() : id, prompt, topic, difficulty);
        Objects.requireNonNull(choices, "choices");
        if (choices.size() < 2) {
            throw new IllegalArgumentException("MCQ requires at least two choices");
        }
        if (correctIndex < 0 || correctIndex >= choices.size()) {
            throw new IllegalArgumentException("correctIndex out of range");
        }
        this.choices = List.copyOf(choices);
        this.correctIndex = correctIndex;
    }

    public List<String> getChoices() {
        return Collections.unmodifiableList(new ArrayList<>(choices));
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    @Override
    public String getType() {
        return "MCQ";
    }

    @Override
    public String getCorrectAnswer() {
        return choices.get(correctIndex);
    }
}
