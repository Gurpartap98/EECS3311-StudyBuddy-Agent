package studybuddy.quiz;

import studybuddy.domain.MCQQuestion;
import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;

/** Concrete creator for {@link MCQQuestion} products. */
public final class MCQQuestionFactory extends QuizFactory {

    @Override
    public Question createQuestion(QuestionSpec spec) {
        if (spec.getKind() != QuestionSpec.Kind.MCQ) {
            throw new IllegalArgumentException("MCQQuestionFactory requires Kind.MCQ");
        }
        return new MCQQuestion(
                spec.getPrompt(),
                spec.getTopic(),
                spec.getDifficulty(),
                spec.getChoices(),
                spec.getCorrectIndex());
    }
}
