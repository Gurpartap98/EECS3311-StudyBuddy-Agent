package studybuddy.quiz;

import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.ShortAnswerQuestion;

/** Concrete creator for {@link ShortAnswerQuestion} products. */
public final class ShortAnswerQuestionFactory extends QuizFactory {

    @Override
    public Question createQuestion(QuestionSpec spec) {
        if (spec.getKind() != QuestionSpec.Kind.SHORT_ANSWER) {
            throw new IllegalArgumentException("ShortAnswerQuestionFactory requires Kind.SHORT_ANSWER");
        }
        return new ShortAnswerQuestion(
                spec.getPrompt(),
                spec.getTopic(),
                spec.getDifficulty(),
                spec.getExpectedAnswer());
    }
}
