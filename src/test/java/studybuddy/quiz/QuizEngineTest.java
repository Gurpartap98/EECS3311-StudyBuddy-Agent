package studybuddy.quiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;

class QuizEngineTest {

    @Test
    void gradesMcqByLetterAndRecordsMisses() {
        QuizFactory factory = new MCQQuestionFactory();
        Question q1 = factory.createQuestion(QuestionSpec.mcq(
                "What is FCFS?",
                "CPU Scheduling",
                "easy",
                List.of("A queue discipline", "A disk format", "A page table", "A mutex"),
                0));
        Question q2 = factory.createQuestion(QuestionSpec.mcq(
                "Round Robin uses?",
                "CPU Scheduling",
                "easy",
                List.of("Aging", "Time quantum", "Belady", "Thrashing"),
                1));
        Quiz quiz = new Quiz("course-1", "Unit test quiz", List.of(q1, q2));

        QuizEngine engine = new QuizEngine();
        QuizEngine.GradeReport report = engine.grade(
                quiz,
                Map.of(q1.getId(), "a", q2.getId(), "a"));

        assertEquals(2, report.total());
        assertEquals(1, report.correctCount());
        assertEquals(50.0, report.scorePercent(), 0.01);
        assertTrue(report.results().get(0).correct());
        assertFalse(report.results().get(1).correct());
    }
}
