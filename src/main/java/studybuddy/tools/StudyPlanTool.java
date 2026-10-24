package studybuddy.tools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.StudyPlan;
import studybuddy.llm.LLMException;
import studybuddy.progress.ProgressTracker;

/** Builds a multi-session study plan (F06). */
public final class StudyPlanTool implements Tool {

    private final ProgressTracker progressTracker;

    public StudyPlanTool(ProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }

    @Override
    public String name() {
        return "plan";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String courseId = ctx.requireString("courseId");
        double hours = Double.parseDouble(ctx.optionalString("hoursPerDay", "2"));
        LocalDate exam = LocalDate.parse(ctx.optionalString("examDate", LocalDate.now().plusWeeks(2).toString()));
        String focus = ctx.optionalString("focusTopic", "");

        if (ctx.getStore().findCourse(courseId).isEmpty()) {
            return Result.fail("Course not found: " + courseId);
        }

        List<String> weak = progressTracker.weakTopics(courseId);
        PlanSpec spec = new PlanSpec(courseId, hours, exam, focus);

        String prompt = """
                Create a brief day-by-day study plan until %s.
                Hours/day: %.1f. Focus: %s. Weak topics: %s.
                Return lines: YYYY-MM-DD | topic | task | minutes
                """.formatted(exam, hours, focus.isBlank() ? "general review" : focus, weak);

        try {
            String raw = ctx.getLlm().generate(prompt);
            List<StudyPlan.Session> sessions = parseSessions(raw, exam, hours, weak, focus);
            StudyPlan plan = new StudyPlan(courseId, exam, hours, sessions);
            ctx.getStore().save(plan);
            return Result.ok("Saved plan " + plan.getId(), Map.of("plan", plan, "raw", raw));
        } catch (LLMException ex) {
            List<StudyPlan.Session> sessions = fallbackSessions(exam, hours, weak, focus);
            StudyPlan plan = new StudyPlan(courseId, exam, hours, sessions);
            ctx.getStore().save(plan);
            return Result.ok(
                    "Saved deterministic plan " + plan.getId() + " (LLM unavailable)",
                    Map.of("plan", plan, "llmError", ex.getMessage()));
        }
    }

    private List<StudyPlan.Session> parseSessions(
            String raw, LocalDate exam, double hours, List<String> weak, String focus) {
        List<StudyPlan.Session> sessions = new ArrayList<>();
        if (raw != null) {
            for (String line : raw.split("\\R")) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String topic = parts[1].trim();
                    String task = parts[2].trim();
                    int minutes = Integer.parseInt(parts[3].trim().replaceAll("[^0-9]", ""));
                    sessions.add(new StudyPlan.Session(date, topic, task, minutes));
                } catch (RuntimeException ignored) {
                    // skip malformed lines
                }
            }
        }
        if (sessions.isEmpty()) {
            return fallbackSessions(exam, hours, weak, focus);
        }
        return sessions;
    }

    private List<StudyPlan.Session> fallbackSessions(
            LocalDate exam, double hours, List<String> weak, String focus) {
        List<StudyPlan.Session> sessions = new ArrayList<>();
        int minutes = Math.max(30, (int) Math.round(hours * 60));
        LocalDate day = LocalDate.now();
        for (int i = 0; i < 5 && !day.isAfter(exam); i++) {
            String topic = !weak.isEmpty()
                    ? weak.get(i % weak.size())
                    : (focus.isBlank() ? "general review" : focus);
            sessions.add(new StudyPlan.Session(day, topic, "Review notes and practice questions", minutes));
            day = day.plusDays(1);
        }
        return sessions;
    }
}
