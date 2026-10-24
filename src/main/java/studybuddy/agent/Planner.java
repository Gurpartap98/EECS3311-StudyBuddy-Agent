package studybuddy.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a natural-language study goal into an ordered tool plan (F12).
 * This scaffold uses simple keyword routing; Stage 2 later can ask the LLM to plan.
 */
public final class Planner {

    public record PlannedStep(String toolName, String reason) {
    }

    public List<PlannedStep> plan(String goal) {
        String g = goal == null ? "" : goal.toLowerCase(Locale.ROOT);
        List<PlannedStep> steps = new ArrayList<>();

        if (g.contains("summar")) {
            steps.add(new PlannedStep("summarize", "User asked for a summary"));
        }
        if (g.contains("concept") || g.contains("key term")) {
            steps.add(new PlannedStep("concepts", "User asked to extract concepts"));
        }
        if (g.contains("flashcard") || g.contains("flash card")) {
            steps.add(new PlannedStep("flashcards", "User asked for flashcards"));
        }
        if (g.contains("quiz") || g.contains("test me") || g.contains("mcq")) {
            steps.add(new PlannedStep("quiz", "User asked for a quiz"));
        }
        if (g.contains("plan") || g.contains("schedule")) {
            steps.add(new PlannedStep("plan", "User asked for a study plan"));
        }
        if (g.contains("explain") || g.contains("why")) {
            steps.add(new PlannedStep("explain", "User asked for an explanation"));
        }
        if (steps.isEmpty() && !g.isBlank()) {
            steps.add(new PlannedStep("retrieve", "Default: retrieve then answer over materials"));
        }
        return steps;
    }
}
