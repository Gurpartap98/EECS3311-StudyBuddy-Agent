package studybuddy;

import studybuddy.agent.AgentController;
import studybuddy.llm.AppConfig;
import studybuddy.llm.LLMClient;
import studybuddy.llm.LLMClientFactory;
import studybuddy.progress.ProgressTracker;
import studybuddy.store.DocumentStore;
import studybuddy.ui.StudyCLI;
import studybuddy.ui.StudyGUI;

/**
 * Entry point for StudyBuddy Agent (EECS 3311 Stage 2).
 * Usage:
 *   mvn -q exec:java -Dexec.args="cli"
 *   mvn -q exec:java -Dexec.args="gui"
 */
public final class StudyBuddyApp {

    private StudyBuddyApp() {
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].trim().toLowerCase() : "gui";
        AgentController agent = bootstrap();
        System.out.println("StudyBuddy LLM: " + agent.getLlm().describe());
        switch (mode) {
            case "cli" -> new StudyCLI(agent).run();
            case "gui" -> new StudyGUI(agent).show();
            case "help", "-h", "--help" -> printHelp();
            default -> {
                System.err.println("Unknown mode: " + mode);
                printHelp();
                System.exit(1);
            }
        }
    }

    static AgentController bootstrap() {
        AppConfig config = new AppConfig();
        DocumentStore store = new DocumentStore();
        ProgressTracker progress = new ProgressTracker();
        LLMClient llm = LLMClientFactory.fromConfig(config);
        return new AgentController(store, llm, progress);
    }

    private static void printHelp() {
        System.out.println("""
                StudyBuddy Agent — EECS 3311
                Usage: StudyBuddyApp <gui|cli|help>

                Configure LLM via .env (copy from .env.example):
                  LLM_PROVIDER=groq|gemini|ollama|stub
                  LLM_API_KEY=...

                Examples:
                  mvn -q exec:java -Dexec.args="cli"
                  mvn -q exec:java -Dexec.args="gui"
                """);
    }
}
