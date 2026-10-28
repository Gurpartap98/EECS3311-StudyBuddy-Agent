package studybuddy.quiz;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import studybuddy.domain.MCQQuestion;
import studybuddy.domain.Question;
import studybuddy.domain.Quiz;
import studybuddy.domain.ShortAnswerQuestion;

/**
 * Deterministic quiz grading (F09). Explanations are left to ExplainTool.
 */
public final class QuizEngine {

    public record QuestionResult(
            String questionId,
            String prompt,
            String topic,
            String studentAnswer,
            String correctAnswer,
            boolean correct) {
    }

    public record GradeReport(
            String quizId,
            int total,
            int correctCount,
            double scorePercent,
            List<QuestionResult> results) {
    }

    /**
     * Grade a quiz attempt.
     *
     * @param quiz quiz with typed questions
     * @param answers map of questionId → student answer text (or MCQ choice letter/text)
     */
    public GradeReport grade(Quiz quiz, Map<String, String> answers) {
        Objects.requireNonNull(quiz, "quiz");
        Map<String, String> submitted = answers == null ? Map.of() : answers;
        List<QuestionResult> results = new ArrayList<>();
        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            String student = submitted.getOrDefault(question.getId(), "");
            boolean ok = isCorrect(question, student);
            if (ok) {
                correctCount++;
            }
            results.add(new QuestionResult(
                    question.getId(),
                    question.getPrompt(),
                    question.getTopic(),
                    student,
                    question.getCorrectAnswer(),
                    ok));
        }

        int total = quiz.size();
        double percent = total == 0 ? 0.0 : (100.0 * correctCount / total);
        return new GradeReport(quiz.getId(), total, correctCount, percent, List.copyOf(results));
    }

    private boolean isCorrect(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return false;
        }
        if (question instanceof MCQQuestion mcq) {
            return gradeMcq(mcq, studentAnswer.trim());
        }
        if (question instanceof ShortAnswerQuestion sa) {
            return sa.matches(studentAnswer);
        }
        return question.getCorrectAnswer().equalsIgnoreCase(studentAnswer.trim());
    }

    private boolean gradeMcq(MCQQuestion mcq, String studentAnswer) {
        String normalized = studentAnswer.trim();
        // Accept index (0-based or 1-based), letter a/b/c/d, or full choice text
        if (normalized.matches("\\d+")) {
            int idx = Integer.parseInt(normalized);
            if (idx >= 1 && idx <= mcq.getChoices().size()) {
                idx = idx - 1;
            }
            return idx == mcq.getCorrectIndex();
        }
        if (normalized.length() == 1 && Character.isLetter(normalized.charAt(0))) {
            int idx = Character.toLowerCase(normalized.charAt(0)) - 'a';
            return idx == mcq.getCorrectIndex();
        }
        String correct = mcq.getCorrectAnswer();
        return correct.equalsIgnoreCase(normalized);
    }

    /** Convenience: build answers map from parallel lists. */
    public static Map<String, String> zipAnswers(List<String> questionIds, List<String> answers) {
        Map<String, String> map = new LinkedHashMap<>();
        if (questionIds == null || answers == null) {
            return map;
        }
        int n = Math.min(questionIds.size(), answers.size());
        for (int i = 0; i < n; i++) {
            map.put(questionIds.get(i), answers.get(i));
        }
        return map;
    }
}
