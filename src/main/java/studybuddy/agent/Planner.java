package studybuddy.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a natural-language study goal into an ordered multi-step tool plan (F12).
 */
public final class Planner {

    private static final Pattern UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public record PlannedStep(String toolName, String reason) {
    }

    public record ParsedGoal(List<PlannedStep> steps, String courseId, String materialId, String question) {
    }

    public List<PlannedStep> plan(String goal) {
        return parse(goal).steps();
    }

    public ParsedGoal parse(String goal) {
        String g = goal == null ? "" : goal.toLowerCase(Locale.ROOT);
        Set<String> ordered = new LinkedHashSet<>();

        // Multi-step compounds: "summarize then make flashcards"
        if (g.contains("summar")) {
            ordered.add("summarize");
        }
        if (g.contains("concept") || g.contains("key term")) {
            ordered.add("concepts");
        }
        if (g.contains("flashcard") || g.contains("flash card")) {
            ordered.add("flashcards");
        }
        if (g.contains("quiz") || g.contains("test me") || g.contains("mcq")) {
            ordered.add("quiz");
        }
        if (g.contains("study plan") || g.contains("schedule") || g.matches(".*\\bplan\\b.*")) {
            ordered.add("plan");
        }
        if (g.contains("explain") && (g.contains("answer") || g.contains("why") || g.contains("wrong"))) {
            ordered.add("explain");
        }
        if (ordered.isEmpty() && !g.isBlank()) {
            ordered.add("retrieve");
        }

        List<PlannedStep> steps = new ArrayList<>();
        for (String tool : ordered) {
            steps.add(new PlannedStep(tool, reasonFor(tool)));
        }

        String materialId = firstUuid(goal);
        String courseId = secondUuid(goal);
        if (courseId == null && materialId != null && g.contains("course")) {
            // heuristic only — UIs usually pass IDs via dedicated commands
            courseId = materialId;
            materialId = null;
        }
        return new ParsedGoal(steps, courseId, materialId, goal == null ? "" : goal.trim());
    }

    private static String reasonFor(String tool) {
        return switch (tool) {
            case "summarize" -> "User asked for a summary";
            case "concepts" -> "User asked to extract concepts";
            case "flashcards" -> "User asked for flashcards";
            case "quiz" -> "User asked for a quiz";
            case "plan" -> "User asked for a study plan";
            case "explain" -> "User asked for an explanation";
            default -> "Default: retrieve then answer over materials";
        };
    }

    private static String firstUuid(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = UUID.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static String secondUuid(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = UUID.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.find() ? matcher.group() : null;
    }
}
