package studybuddy.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import studybuddy.domain.Course;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;
import studybuddy.domain.QuizSpec;
import studybuddy.llm.LLMClient;
import studybuddy.progress.ProgressEvent;
import studybuddy.progress.ProgressTracker;
import studybuddy.quiz.MCQQuestionFactory;
import studybuddy.quiz.QuizEngine;
import studybuddy.quiz.QuizFactory;
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
    private final QuizEngine quizEngine = new QuizEngine();
    private final ExplainTool explainTool = new ExplainTool();

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
        toolManager.register(explainTool);
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

    /**
     * Deterministic demo quiz built via Factory Method (useful without an LLM key).
     */
    public Result createDemoQuiz(String courseId) {
        if (store.findCourse(courseId).isEmpty()) {
            return Result.fail("Course not found: " + courseId);
        }
        QuizFactory factory = new MCQQuestionFactory();
        List<Question> questions = new ArrayList<>();
        questions.add(factory.createQuestion(QuestionSpec.mcq(
                "Which scheduler can cause the convoy effect?",
                "CPU Scheduling",
                "easy",
                List.of("Round Robin", "FCFS", "Multilevel feedback", "Priority with aging"),
                1)));
        questions.add(factory.createQuestion(QuestionSpec.mcq(
                "Round Robin is characterized by which parameter?",
                "CPU Scheduling",
                "easy",
                List.of("Priority ceiling", "Time quantum", "Working set", "Belady anomaly"),
                1)));
        questions.add(factory.createQuestion(QuestionSpec.mcq(
                "SJF optimizes which metric when runtimes are known?",
                "CPU Scheduling",
                "medium",
                List.of("Throughput only", "Average waiting time", "Context-switch cost", "Disk latency"),
                1)));
        Quiz quiz = new Quiz(courseId, "Demo CPU Scheduling Quiz", questions);
        store.save(quiz);
        return Result.ok(
                "Created demo quiz " + quiz.getId() + " with " + quiz.size() + " MCQ questions",
                Map.of("quiz", quiz));
    }

    /**
     * F09 — grade answers with {@link QuizEngine}, record progress (F10/F11), explain misses.
     *
     * @param answers map of questionId → student answer
     */
    public Result gradeQuiz(String quizId, Map<String, String> answers) {
        Quiz quiz = store.findQuiz(quizId).orElse(null);
        if (quiz == null) {
            return Result.fail("Quiz not found: " + quizId);
        }
        QuizEngine.GradeReport report = quizEngine.grade(quiz, answers);

        // Aggregate per-topic scores for ProgressTracker / weak topics
        Map<String, List<Boolean>> byTopic = new LinkedHashMap<>();
        for (QuizEngine.QuestionResult qr : report.results()) {
            String topic = qr.topic().isBlank() ? "general" : qr.topic();
            byTopic.computeIfAbsent(topic, t -> new ArrayList<>()).add(qr.correct());
        }
        for (Map.Entry<String, List<Boolean>> entry : byTopic.entrySet()) {
            long ok = entry.getValue().stream().filter(Boolean::booleanValue).count();
            double pct = 100.0 * ok / entry.getValue().size();
            progressTracker.record(new ProgressEvent(quiz.getCourseId(), entry.getKey(), pct));
        }

        StringBuilder message = new StringBuilder();
        message.append("Score: ")
                .append(report.correctCount())
                .append('/')
                .append(report.total())
                .append(" (")
                .append(String.format("%.0f", report.scorePercent()))
                .append("%)\n");

        List<String> explanations = new ArrayList<>();
        for (QuizEngine.QuestionResult qr : report.results()) {
            message.append(qr.correct() ? "[OK] " : "[MISS] ")
                    .append(qr.prompt())
                    .append(" → you: ")
                    .append(qr.studentAnswer().isBlank() ? "(blank)" : qr.studentAnswer())
                    .append("; correct: ")
                    .append(qr.correctAnswer())
                    .append('\n');
            if (!qr.correct()) {
                String explanation = explainTool.explain(qr.prompt(), qr.correctAnswer(), qr.studentAnswer());
                explanations.add(explanation);
                message.append("  Explain: ").append(explanation).append('\n');
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("report", report);
        data.put("explanations", explanations);
        data.put("weakTopics", progressTracker.weakTopics(quiz.getCourseId()));
        return Result.ok(message.toString().trim(), data);
    }

    public QuizEngine getQuizEngine() {
        return quizEngine;
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
