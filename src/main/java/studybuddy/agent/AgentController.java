package studybuddy.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import studybuddy.domain.Course;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.QuizSpec;
import studybuddy.llm.LLMClient;
import studybuddy.progress.ProgressTracker;
import studybuddy.store.DocumentStore;
import studybuddy.tools.ConceptTool;
import studybuddy.tools.ExplainTool;
import studybuddy.tools.FlashcardTool;
import studybuddy.tools.MaterialImportTool;
import studybuddy.tools.QuizTool;
import studybuddy.tools.Result;
import studybuddy.tools.RetrievalTool;
import studybuddy.tools.StudyPlanTool;
import studybuddy.tools.SummarizeTool;
import studybuddy.tools.ToolContext;

/**
 * Facade: single UI entry point for GUI and CLI (MVC controller + Facade pattern).
 */
public final class AgentController {

    private final DocumentStore store;
    private final LLMClient llm;
    private final ToolManager toolManager;
    private final Planner planner;
    private final ProgressTracker progressTracker;

    public AgentController(DocumentStore store, LLMClient llm, ProgressTracker progressTracker) {
        this.store = Objects.requireNonNull(store, "store");
        this.llm = Objects.requireNonNull(llm, "llm");
        this.progressTracker = Objects.requireNonNull(progressTracker, "progressTracker");
        this.toolManager = new ToolManager();
        this.planner = new Planner();
        registerDefaultTools();
    }

    private void registerDefaultTools() {
        toolManager.register(new MaterialImportTool());
        toolManager.register(new SummarizeTool());
        toolManager.register(new ConceptTool());
        toolManager.register(new RetrievalTool());
        toolManager.register(new FlashcardTool());
        toolManager.register(new QuizTool());
        toolManager.register(new ExplainTool());
        toolManager.register(new StudyPlanTool(progressTracker));
    }

    public DocumentStore getStore() {
        return store;
    }

    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public LLMClient getLlm() {
        return llm;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    /** F01 — create and persist a course. */
    public Result createCourse(String name, String code, String term) {
        try {
            Course course = new Course(name, code, term);
            store.save(course);
            return Result.ok("Created course " + course.getId(), Map.of("course", course));
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    public List<Course> listCourses() {
        return store.listCourses();
    }

    /** F02 — upload material via MaterialImportTool.execute. */
    public Result upload(String courseId, String path) {
        return toolManager.executeTool("import", ctx(Map.of("courseId", courseId, "path", path)));
    }

    /** F03 */
    public Result summarize(String materialId) {
        return toolManager.executeTool("summarize", ctx(Map.of("materialId", materialId)));
    }

    /** F04 */
    public Result extractConcepts(String materialId) {
        return toolManager.executeTool("concepts", ctx(Map.of("materialId", materialId)));
    }

    /** F05 */
    public Result ask(String question) {
        return ask(question, "");
    }

    public Result ask(String question, String courseId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("question", question);
        if (courseId != null && !courseId.isBlank()) {
            params.put("courseId", courseId);
        }
        return toolManager.executeTool("retrieve", ctx(params));
    }

    /** F06 */
    public Result generatePlan(PlanSpec spec) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("courseId", spec.getCourseId());
        params.put("hoursPerDay", String.valueOf(spec.getHoursPerDay()));
        if (spec.getExamDate() != null) {
            params.put("examDate", spec.getExamDate().toString());
        }
        if (spec.getFocusTopic() != null && !spec.getFocusTopic().isBlank()) {
            params.put("focusTopic", spec.getFocusTopic());
        }
        return toolManager.executeTool("plan", ctx(params));
    }

    /** F07 */
    public Result generateFlashcards(String materialId, int n) {
        return toolManager.executeTool(
                "flashcards", ctx(Map.of("materialId", materialId, "count", n)));
    }

    /** F08 */
    public Result generateQuiz(QuizSpec spec) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("courseId", spec.getCourseId());
        params.put("count", spec.getQuestionCount());
        params.put("difficulty", spec.getDifficulty());
        params.put("kind", spec.getKind().name());
        if (spec.getMaterialId() != null && !spec.getMaterialId().isBlank()) {
            params.put("materialId", spec.getMaterialId());
        }
        return toolManager.executeTool("quiz", ctx(params));
    }

    /** F12 — multi-step NL command handling via Planner + ToolManager. */
    public Result handleRequest(String text) {
        List<Planner.PlannedStep> steps = planner.plan(text);
        if (steps.isEmpty()) {
            return Result.fail("Could not plan any tools for that request.");
        }
        StringBuilder log = new StringBuilder("Plan:\n");
        for (Planner.PlannedStep step : steps) {
            log.append("- ").append(step.toolName()).append(": ").append(step.reason()).append('\n');
        }
        // For scaffold: if the only step is retrieve, execute ask(); otherwise report the plan.
        if (steps.size() == 1 && "retrieve".equals(steps.get(0).toolName())) {
            Result answer = ask(text);
            return Result.ok(
                    log + "\n" + answer.getMessage(),
                    Map.of("plan", steps, "answer", answer));
        }
        return Result.ok(
                log + "\nUse the dedicated commands (summarize, concepts, quiz, ...) "
                        + "with course/material IDs for multi-step tools in this scaffold.",
                Map.of("plan", steps));
    }

    private ToolContext ctx(Map<String, Object> params) {
        return new ToolContext(store, llm, params);
    }
}
