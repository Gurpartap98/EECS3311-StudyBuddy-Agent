package studybuddy.llm;

/**
 * Central prompt construction for agent tools (summarize, QA, concepts, flashcards, quiz).
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildSummaryPrompt(String notes) {
        return """
                Summarize the following study notes in clear bullet points for a university student.
                Keep important terms and definitions.

                NOTES:
                %s
                """.formatted(truncate(notes, 6000));
    }

    public static String buildConceptPrompt(String notes) {
        return """
                Extract key concepts from these notes.
                Return one concept per line as: NAME — short gloss

                NOTES:
                %s
                """.formatted(truncate(notes, 6000));
    }

    public static String buildQA(String question, String notesContext) {
        return """
                Answer the student question using ONLY the provided notes.
                Cite material titles. If the notes do not contain the answer, say so.

                QUESTION: %s

                NOTES:
                %s
                """.formatted(question, truncate(notesContext, 8000));
    }

    public static String buildFlashcardPrompt(String notes, int count) {
        return """
                Create %d flashcards from these notes.
                Format each card as: Q: ... | A: ...

                NOTES:
                %s
                """.formatted(count, truncate(notes, 6000));
    }

    public static String buildQuizPrompt(int count, String kind, String difficulty, String notes) {
        return """
                Generate %d %s study questions at %s difficulty from the notes.
                For MCQ use lines: Q: ... | CHOICES: a) .. ; b) .. ; c) .. ; d) .. | ANSWER: a
                For SHORT_ANSWER use lines: Q: ... | A: ...

                NOTES:
                %s
                """.formatted(count, kind, difficulty, truncate(notes, 6000));
    }

    public static String buildPlanPrompt(
            String examDate, double hoursPerDay, String focus, String weakTopics) {
        return """
                Create a brief day-by-day study plan until %s.
                Hours/day: %.1f. Focus: %s. Weak topics: %s.
                Return lines: YYYY-MM-DD | topic | task | minutes
                """.formatted(
                examDate,
                hoursPerDay,
                focus == null || focus.isBlank() ? "general review" : focus,
                weakTopics == null || weakTopics.isBlank() ? "(none yet)" : weakTopics);
    }

    public static String buildExplainPrompt(String question, String correct, String student) {
        return """
                Explain why the correct answer is right and the student answer is wrong.
                Question: %s
                Correct: %s
                Student: %s
                Keep it under 120 words.
                """.formatted(question, correct, student);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "\n...[truncated]";
    }
}
