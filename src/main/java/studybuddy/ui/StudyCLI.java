package studybuddy.ui;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import studybuddy.agent.AgentController;
import studybuddy.domain.Course;
import studybuddy.domain.Material;
import studybuddy.domain.Question;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;
import studybuddy.domain.QuizSpec;
import studybuddy.tools.Result;

/**
 * Command-line view that delegates all work to {@link AgentController} (MVC).
 * Supports interactive stdin and non-interactive {@code -input task.txt} batch mode.
 */
public final class StudyCLI {

    private final AgentController agent;
    private final Scanner in;
    private final PrintStream out;
    private final boolean interactive;

    /** Last IDs for $COURSE / $MATERIAL / $QUIZ substitution in task files. */
    private String lastCourseId = "";
    private String lastMaterialId = "";
    private String lastQuizId = "";

    public StudyCLI(AgentController agent) {
        this(agent, new Scanner(System.in), System.out, true);
    }

    public StudyCLI(AgentController agent, Scanner in, PrintStream out) {
        this(agent, in, out, true);
    }

    public StudyCLI(AgentController agent, Scanner in, PrintStream out, boolean interactive) {
        this.agent = agent;
        this.in = in;
        this.out = out;
        this.interactive = interactive;
    }

    public void run() {
        out.println("StudyBuddy CLI — type 'help' for commands, 'quit' to exit.");
        out.println("LLM: " + agent.getLlm().describe());
        while (true) {
            out.print("studybuddy> ");
            if (!in.hasNextLine()) {
                break;
            }
            String line = in.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                out.println("Bye.");
                break;
            }
            handle(expandPlaceholders(line));
        }
    }

    /**
     * Batch mode: read CLI commands from a task file (Stage 2/3 standalone CLI requirement).
     * Lines starting with {@code #} are comments. Supports {@code $COURSE}, {@code $MATERIAL}, {@code $QUIZ}.
     */
    public void runFromFile(Path taskFile) {
        out.println("StudyBuddy CLI — batch mode from file: " + taskFile.toAbsolutePath());
        out.println("LLM: " + agent.getLlm().describe());
        List<String> lines;
        try {
            lines = Files.readAllLines(taskFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot read task file: " + ex.getMessage(), ex);
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                out.println("Bye.");
                break;
            }
            String expanded = expandPlaceholders(line);
            out.println("studybuddy> " + expanded);
            handle(expanded);
        }
        out.println("Batch finished.");
    }

    private String expandPlaceholders(String line) {
        String outLine = line;
        if (!lastCourseId.isBlank()) {
            outLine = outLine.replace("$COURSE", lastCourseId);
        }
        if (!lastMaterialId.isBlank()) {
            outLine = outLine.replace("$MATERIAL", lastMaterialId);
        }
        if (!lastQuizId.isBlank()) {
            outLine = outLine.replace("$QUIZ", lastQuizId);
        }
        return outLine;
    }

    private void handle(String line) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1].trim() : "";
        try {
            switch (cmd) {
                case "help" -> printHelp();
                case "courses" -> listCourses();
                case "course" -> createCourse(args);
                case "rename" -> renameCourse(args);
                case "archive" -> printResult(agent.archiveCourse(args));
                case "delete" -> printResult(agent.deleteCourse(args));
                case "upload" -> upload(args);
                case "materials" -> listMaterials(args);
                case "summarize" -> printResult(agent.summarize(expandId(args)));
                case "concepts" -> printResult(agent.extractConcepts(expandId(args)));
                case "ask" -> printResult(agent.ask(args));
                case "flashcards" -> flashcards(args);
                case "review" -> reviewFlashcards(args);
                case "quiz" -> quiz(args);
                case "demo-quiz" -> demoQuiz(args);
                case "quizzes" -> listQuizzes(args);
                case "take-quiz" -> takeQuiz(args);
                case "grade" -> grade(args);
                case "plan" -> plan(args);
                case "weak" -> weak(args);
                case "history" -> history(args);
                case "ailog" -> out.println(studybuddy.llm.AiUsageLog.get().summary());
                case "nl" -> printResult(agent.handleRequest(args));
                default -> printResult(agent.handleRequest(line));
            }
        } catch (RuntimeException ex) {
            out.println("Error: " + ex.getMessage());
        }
    }

    private String expandId(String id) {
        if (id == null) {
            return "";
        }
        return expandPlaceholders(id.trim());
    }

    private void printHelp() {
        out.println("""
                Commands:
                  courses
                  course <name> [|code|term]
                  rename <courseId> <new name>
                  archive <courseId>
                  delete <courseId>
                  upload <courseId> <path>
                  materials <courseId>
                  summarize <materialId>
                  concepts <materialId>
                  ask <question>
                  flashcards <materialId> [count]
                  review <materialId>         (flip through saved flashcards)
                  quiz <courseId> [materialId] [count]
                  demo-quiz <courseId>
                  quizzes <courseId>
                  take-quiz <quizId>          (interactive prompts, then grades)
                  grade <quizId> <a1,a2,...>   (answers by question order: a/b/c/d or text)
                  plan <courseId> [hours] [examDate]
                  weak <courseId>
                  history [courseId]
                  ailog
                  nl <natural language request>
                  quit

                Batch / task-file placeholders:
                  $COURSE    last created/selected course id
                  $MATERIAL  last uploaded material id
                  $QUIZ      last created demo/AI quiz id

                Batch example:
                  java -jar Agent.jar -LLMAPI -input examples/task.txt
                """);
    }

    private void listCourses() {
        List<Course> courses = agent.listCourses();
        if (courses.isEmpty()) {
            out.println("(no courses yet)");
            return;
        }
        for (Course c : courses) {
            out.printf("%s  %s  [%s %s]%s%n",
                    c.getId(),
                    c.getName(),
                    c.getCode(),
                    c.getTerm(),
                    c.isArchived() ? " (archived)" : "");
        }
    }

    private void createCourse(String args) {
        if (args.isBlank()) {
            out.println("Usage: course <name> [|code|term]");
            return;
        }
        String[] bits = args.split("\\|");
        String name = bits[0].trim();
        String code = bits.length > 1 ? bits[1].trim() : "";
        String term = bits.length > 2 ? bits[2].trim() : "";
        Result result = agent.createCourse(name, code, term);
        if (result.isSuccess() && result.get("course") instanceof Course created) {
            agent.setActiveCourseId(created.getId());
            lastCourseId = created.getId();
        }
        printResult(result);
    }

    private void renameCourse(String args) {
        String[] bits = args.split("\\s+", 2);
        if (bits.length < 2) {
            out.println("Usage: rename <courseId> <new name>");
            return;
        }
        printResult(agent.renameCourse(bits[0], bits[1]));
    }

    private void upload(String args) {
        String[] bits = args.split("\\s+", 2);
        if (bits.length < 2) {
            out.println("Usage: upload <courseId> <path>");
            return;
        }
        String courseId = expandId(bits[0]);
        agent.setActiveCourseId(courseId);
        lastCourseId = courseId;
        Result result = agent.upload(courseId, bits[1]);
        if (result.isSuccess()) {
            Object materialId = result.getData().get("materialId");
            if (materialId != null) {
                lastMaterialId = String.valueOf(materialId);
                agent.setActiveMaterialId(lastMaterialId);
            }
        }
        printResult(result);
    }

    private void listMaterials(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: materials <courseId>");
            return;
        }
        String cid = expandId(courseId);
        List<Material> materials = agent.getStore().listMaterials(cid);
        if (materials.isEmpty()) {
            out.println("(no materials)");
            return;
        }
        for (Material m : materials) {
            out.printf("%s  %s  (%d chars)%n", m.getId(), m.getTitle(), m.getExtractedText().length());
        }
    }

    private void flashcards(String args) {
        String[] bits = args.split("\\s+");
        if (bits.length < 1 || bits[0].isBlank()) {
            out.println("Usage: flashcards <materialId> [count]");
            return;
        }
        int n = bits.length > 1 ? Integer.parseInt(bits[1]) : 8;
        printResult(agent.generateFlashcards(bits[0], n));
    }

    private void reviewFlashcards(String materialId) {
        if (materialId.isBlank()) {
            out.println("Usage: review <materialId>");
            return;
        }
        var cards = agent.getStore().listFlashcards(materialId);
        if (cards.isEmpty()) {
            out.println("(no flashcards — run flashcards <materialId> first)");
            return;
        }
        out.println("Reviewing " + cards.size() + " cards. Press Enter to flip/next, 'q' to stop.");
        for (int i = 0; i < cards.size(); i++) {
            var card = cards.get(i);
            out.println();
            out.println("Q" + (i + 1) + ": " + card.getFront());
            out.print("(Enter to show answer) ");
            String line = in.hasNextLine() ? in.nextLine() : "";
            if ("q".equalsIgnoreCase(line.trim())) {
                break;
            }
            out.println("A: " + card.getBack());
            out.print("(Enter for next) ");
            line = in.hasNextLine() ? in.nextLine() : "";
            if ("q".equalsIgnoreCase(line.trim())) {
                break;
            }
        }
        out.println("Review done.");
    }

    private void demoQuiz(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: demo-quiz <courseId>");
            return;
        }
        String cid = expandId(courseId);
        Result created = agent.createDemoQuiz(cid);
        printResult(created);
        if (!created.isSuccess()) {
            return;
        }
        Object quizObj = created.getData().get("quiz");
        String quizId = created.getData().get("quizId") == null
                ? ""
                : String.valueOf(created.getData().get("quizId"));
        if (quizObj instanceof Quiz quiz) {
            quizId = quiz.getId();
        }
        if (!quizId.isBlank()) {
            lastQuizId = quizId;
        }
        // Interactive sessions auto-start take-quiz; batch (-input) uses grade $QUIZ ...
        if (interactive && !quizId.isBlank()) {
            out.println();
            out.println(">>> Starting the quiz now (questions appear below). <<<");
            out.println();
            takeQuiz(quizId);
        } else if (!quizId.isBlank()) {
            out.println("Batch mode: quiz created. Use: grade $QUIZ b,b,a   (or take-quiz in interactive CLI)");
        }
    }

    private void quiz(String args) {
        String[] bits = args.split("\\s+");
        if (bits.length < 1 || bits[0].isBlank()) {
            out.println("Usage: quiz <courseId> [materialId] [count]");
            return;
        }
        String courseId = expandId(bits[0]);
        String materialId = bits.length > 1 ? expandId(bits[1]) : "";
        int count = bits.length > 2 ? Integer.parseInt(bits[2]) : 5;
        QuizSpec spec = new QuizSpec(courseId, materialId, count, "medium", QuestionSpec.Kind.MCQ, "");
        Result result = agent.generateQuiz(spec);
        if (result.isSuccess() && result.getData().get("quiz") instanceof Quiz q) {
            lastQuizId = q.getId();
        }
        printResult(result);
    }

    private void listQuizzes(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: quizzes <courseId>");
            return;
        }
        List<Quiz> quizzes = agent.getStore().listQuizzes(expandId(courseId));
        if (quizzes.isEmpty()) {
            out.println("(no quizzes)");
            return;
        }
        for (Quiz q : quizzes) {
            out.printf("%s  %s  (%d questions)%n", q.getId(), q.getTitle(), q.size());
            int i = 1;
            for (Question question : q.getQuestions()) {
                out.printf("   %d. [%s] %s%n", i++, question.getType(), question.getPrompt());
            }
        }
    }

    private void takeQuiz(String idOrCourseId) {
        if (idOrCourseId.isBlank()) {
            out.println("Usage: take-quiz <quizId>");
            out.println("   or: take-quiz <courseId>   (uses the latest quiz for that course)");
            return;
        }
        String key = idOrCourseId.trim();
        Quiz quiz = agent.getStore().findQuiz(key).orElse(null);
        if (quiz == null) {
            // Allow passing courseId by mistake/convenience — pick latest quiz for course
            List<Quiz> forCourse = agent.getStore().listQuizzes(key);
            if (!forCourse.isEmpty()) {
                quiz = forCourse.get(forCourse.size() - 1);
                out.println("Note: using latest quiz for course: " + quiz.getId());
            }
        }
        if (quiz == null) {
            out.println("Quiz not found: " + key);
            out.println("Tip: run  quizzes <courseId>  then  take-quiz <quizId>");
            return;
        }
        out.println("Taking: " + quiz.getTitle() + " (" + quiz.size() + " questions)");
        Map<String, String> answers = new LinkedHashMap<>();
        int n = 1;
        for (Question question : quiz.getQuestions()) {
            out.println();
            out.println(n++ + ". [" + question.getType() + "] " + question.getPrompt());
            if (question instanceof studybuddy.domain.MCQQuestion mcq) {
                char label = 'a';
                for (String choice : mcq.getChoices()) {
                    out.println("   " + label++ + ") " + choice);
                }
                out.print("Your answer (a/b/c/...): ");
            } else {
                out.print("Your answer: ");
            }
            String ans = in.hasNextLine() ? in.nextLine().trim() : "";
            answers.put(question.getId(), ans);
        }
        printResult(agent.gradeQuiz(quiz.getId(), answers));
    }

    private void grade(String args) {
        String[] bits = args.split("\\s+", 2);
        if (bits.length < 2) {
            out.println("Usage: grade <quizId> <a1,a2,...>");
            return;
        }
        String quizKey = expandId(bits[0]);
        Quiz quiz = agent.getStore().findQuiz(quizKey).orElse(null);
        if (quiz == null) {
            List<Quiz> forCourse = agent.getStore().listQuizzes(quizKey);
            if (!forCourse.isEmpty()) {
                quiz = forCourse.get(forCourse.size() - 1);
                quizKey = quiz.getId();
            }
        }
        if (quiz == null) {
            out.println("Quiz not found: " + bits[0]);
            return;
        }
        lastQuizId = quiz.getId();
        String[] rawAnswers = bits[1].split(",");
        Map<String, String> answers = new LinkedHashMap<>();
        List<Question> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            String ans = i < rawAnswers.length ? rawAnswers[i].trim() : "";
            answers.put(questions.get(i).getId(), ans);
        }
        printResult(agent.gradeQuiz(quiz.getId(), answers));
    }

    private void plan(String args) {
        String[] bits = args.split("\\s+");
        if (bits.length < 1 || bits[0].isBlank()) {
            out.println("Usage: plan <courseId> [hours] [examDate]");
            return;
        }
        String courseId = expandId(bits[0]);
        double hours = bits.length > 1 ? Double.parseDouble(bits[1]) : 2.0;
        LocalDate exam = bits.length > 2 ? LocalDate.parse(bits[2]) : LocalDate.now().plusWeeks(2);
        printResult(agent.generatePlan(new PlanSpec(courseId, hours, exam, "")));
    }

    private void weak(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: weak <courseId>");
            return;
        }
        String cid = expandId(courseId);
        List<String> weak = agent.getProgressTracker().weakTopics(cid);
        out.println(weak.isEmpty() ? "(no weak topics yet — take a quiz first)" : weak);
    }

    private void history(String courseId) {
        String cid = courseId.isBlank() ? "" : expandId(courseId);
        var events = cid.isBlank()
                ? agent.getProgressTracker().history()
                : agent.getProgressTracker().historyForCourse(cid);
        if (events.isEmpty()) {
            out.println("(no progress history yet)");
            return;
        }
        for (var e : events) {
            out.printf("%s | course=%s | topic=%s | %.0f%%%n",
                    e.getAt(), e.getCourseId(), e.getTopic(), e.getScorePercent());
        }
    }

    private void printResult(Result result) {
        out.println((result.isSuccess() ? "OK: " : "FAIL: ") + result.getMessage());
    }
}
