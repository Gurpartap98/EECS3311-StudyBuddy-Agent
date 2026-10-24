package studybuddy.quiz;

import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;

/**
 * Abstract creator in the Factory Method pattern for quiz questions.
 */
public abstract class QuizFactory {

    /** Factory Method: subclasses decide which concrete Question to instantiate. */
    public abstract Question createQuestion(QuestionSpec spec);

    /** Convenience: create many questions with the same concrete factory. */
    public java.util.List<Question> createQuestions(java.util.List<QuestionSpec> specs) {
        java.util.List<Question> questions = new java.util.ArrayList<>();
        if (specs == null) {
            return questions;
        }
        for (QuestionSpec spec : specs) {
            questions.add(createQuestion(spec));
        }
        return questions;
    }
}
