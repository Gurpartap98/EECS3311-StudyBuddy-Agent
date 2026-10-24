package studybuddy.tools;

import java.util.Map;
import studybuddy.llm.LLMException;

/**
 * Explains incorrect quiz answers (F09).
 * {@link #explain(String, String, String)} is a helper called from {@link #execute(ToolContext)}.
 */
public final class ExplainTool implements Tool {

    @Override
    public String name() {
        return "explain";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String promptText = ctx.requireString("prompt");
        String correct = ctx.requireString("correct");
        String student = ctx.optionalString("student", "");
        try {
            String viaLlm = ctx.getLlm().generate("""
                    Explain why the correct answer is right and the student answer is wrong.
                    Question: %s
                    Correct: %s
                    Student: %s
                    Keep it under 120 words.
                    """.formatted(promptText, correct, student));
            return Result.ok(viaLlm, Map.of("explanation", viaLlm));
        } catch (LLMException ex) {
            String fallback = explain(promptText, correct, student);
            return Result.ok(fallback, Map.of("explanation", fallback, "llmError", ex.getMessage()));
        }
    }

    /** Helper used by {@link #execute(ToolContext)}. */
    public String explain(String question, String correctAnswer, String studentAnswer) {
        return "For \"" + question + "\", the correct answer is \"" + correctAnswer
                + "\". Your answer was \"" + studentAnswer
                + "\". Review the related notes and compare key terms.";
    }
}
