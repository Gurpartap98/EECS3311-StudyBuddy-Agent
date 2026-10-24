package studybuddy.domain;

import java.util.Objects;

/**
 * Input for quiz generation (F08).
 */
public final class QuizSpec {

    private final String courseId;
    private final String materialId;
    private final int questionCount;
    private final String difficulty;
    private final QuestionSpec.Kind kind;
    private final String focusTopic;

    public QuizSpec(
            String courseId,
            String materialId,
            int questionCount,
            String difficulty,
            QuestionSpec.Kind kind,
            String focusTopic) {
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.materialId = materialId;
        if (questionCount <= 0) {
            throw new IllegalArgumentException("questionCount must be positive");
        }
        this.questionCount = questionCount;
        this.difficulty = difficulty == null ? "medium" : difficulty;
        this.kind = kind == null ? QuestionSpec.Kind.MCQ : kind;
        this.focusTopic = focusTopic == null ? "" : focusTopic;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public QuestionSpec.Kind getKind() {
        return kind;
    }

    public String getFocusTopic() {
        return focusTopic;
    }
}
