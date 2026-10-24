package studybuddy.ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import studybuddy.agent.AgentController;
import studybuddy.domain.Course;
import studybuddy.domain.Material;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.QuizSpec;
import studybuddy.tools.Result;

/**
 * Command-line view that delegates all work to {@link AgentController} (MVC).
 */
public final class StudyCLI {

    private final AgentController agent;
    private final Scanner in;
    private final PrintStream out;

    public StudyCLI(AgentController agent) {
        this(agent, new Scanner(System.in), System.out);
    }

    public StudyCLI(AgentController agent, Scanner in, PrintStream out) {
        this.agent = agent;
        this.in = in;
        this.out = out;
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
            handle(line);
        }
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
                case "upload" -> upload(args);
                case "materials" -> listMaterials(args);
                case "summarize" -> printResult(agent.summarize(args));
                case "concepts" -> printResult(agent.extractConcepts(args));
                case "ask" -> printResult(agent.ask(args));
                case "flashcards" -> flashcards(args);
                case "quiz" -> quiz(args);
                case "plan" -> plan(args);
                case "weak" -> weak(args);
                case "nl" -> printResult(agent.handleRequest(args));
                default -> printResult(agent.handleRequest(line));
            }
        } catch (RuntimeException ex) {
            out.println("Error: " + ex.getMessage());
        }
    }

    private void printHelp() {
        out.println("""
                Commands:
                  courses
                  course <name> [|code|term]
                  upload <courseId> <path>
                  materials <courseId>
                  summarize <materialId>
                  concepts <materialId>
                  ask <question>
                  flashcards <materialId> [count]
                  quiz <courseId> [materialId] [count]
                  plan <courseId> [hours] [examDate]
                  weak <courseId>
                  nl <natural language request>
                  quit
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
        printResult(agent.createCourse(name, code, term));
    }

    private void upload(String args) {
        String[] bits = args.split("\\s+", 2);
        if (bits.length < 2) {
            out.println("Usage: upload <courseId> <path>");
            return;
        }
        printResult(agent.upload(bits[0], bits[1]));
    }

    private void listMaterials(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: materials <courseId>");
            return;
        }
        List<Material> materials = agent.getStore().listMaterials(courseId);
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

    private void quiz(String args) {
        String[] bits = args.split("\\s+");
        if (bits.length < 1 || bits[0].isBlank()) {
            out.println("Usage: quiz <courseId> [materialId] [count]");
            return;
        }
        String courseId = bits[0];
        String materialId = bits.length > 1 ? bits[1] : "";
        int count = bits.length > 2 ? Integer.parseInt(bits[2]) : 5;
        QuizSpec spec = new QuizSpec(courseId, materialId, count, "medium", QuestionSpec.Kind.MCQ, "");
        printResult(agent.generateQuiz(spec));
    }

    private void plan(String args) {
        String[] bits = args.split("\\s+");
        if (bits.length < 1 || bits[0].isBlank()) {
            out.println("Usage: plan <courseId> [hours] [examDate]");
            return;
        }
        double hours = bits.length > 1 ? Double.parseDouble(bits[1]) : 2.0;
        LocalDate exam = bits.length > 2 ? LocalDate.parse(bits[2]) : LocalDate.now().plusWeeks(2);
        printResult(agent.generatePlan(new PlanSpec(bits[0], hours, exam, "")));
    }

    private void weak(String courseId) {
        if (courseId.isBlank()) {
            out.println("Usage: weak <courseId>");
            return;
        }
        List<String> weak = agent.getProgressTracker().weakTopics(courseId);
        out.println(weak.isEmpty() ? "(no weak topics yet — take a quiz first)" : weak);
    }

    private void printResult(Result result) {
        out.println((result.isSuccess() ? "OK: " : "FAIL: ") + result.getMessage());
    }
}
