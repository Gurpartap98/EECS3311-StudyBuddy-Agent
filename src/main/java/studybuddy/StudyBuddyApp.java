package studybuddy;

import studybuddy.agent.AgentController;
import studybuddy.llm.LLMClient;
import studybuddy.llm.StubLLMClient;
import studybuddy.progress.ProgressTracker;
import studybuddy.store.DocumentStore;
import studybuddy.ui.StudyCLI;

/**
 * Entry point for StudyBuddy Agent (EECS 3311 Stage 2).
 * Usage:
 *   mvn -q exec:java -Dexec.args="cli"
 */
public final class StudyBuddyApp {

    private StudyBuddyApp() {
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].trim().toLowerCase() : "cli";
        AgentController agent = bootstrap();
        switch (mode) {
            case "cli" -> new StudyCLI(agent).run();
            case "gui" -> {
                System.err.println("GUI arrives in the next commit. Use: mvn -q exec:java -Dexec.args=\"cli\"");
                System.exit(1);
            }
            case "help", "-h", "--help" -> printHelp();
            default -> {
                System.err.println("Unknown mode: " + mode);
                printHelp();
                System.exit(1);
            }
        }
    }

    static AgentController bootstrap() {
        DocumentStore store = new DocumentStore();
        ProgressTracker progress = new ProgressTracker();
        LLMClient llm = new StubLLMClient();
        return new AgentController(store, llm, progress);
    }

    private static void printHelp() {
        System.out.println("""
                StudyBuddy Agent — EECS 3311
                Usage: StudyBuddyApp <cli|help>
                Example: mvn -q exec:java -Dexec.args="cli"
                """);
    }
}
