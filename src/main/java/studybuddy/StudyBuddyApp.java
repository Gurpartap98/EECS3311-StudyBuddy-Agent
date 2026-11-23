package studybuddy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import studybuddy.agent.AgentController;
import studybuddy.llm.AiUsageLog;
import studybuddy.llm.AppConfig;
import studybuddy.llm.LLMClient;
import studybuddy.llm.LLMClientFactory;
import studybuddy.progress.ProgressTracker;
import studybuddy.store.DocumentStore;
import studybuddy.store.JsonDocumentStore;
import studybuddy.ui.StudyCLI;
import studybuddy.ui.StudyGUI;

/**
 * Entry point for StudyBuddy Agent (EECS 3311 Stage 2 / Stage 3 CLI).
 *
 * <pre>
 *   java -jar Agent.jar gui
 *   java -jar Agent.jar cli
 *   java -jar Agent.jar -LLMAPI -input examples/task.txt
 *   java -jar Agent.jar cli --input examples/task.txt
 *   mvn -q exec:java -Dexec.args="cli"
 * </pre>
 */
public final class StudyBuddyApp {

    private StudyBuddyApp() {
    }

    public static void main(String[] args) {
        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed.help) {
            printHelp();
            return;
        }
        AgentController agent = bootstrap();
        System.out.println("StudyBuddy LLM: " + agent.getLlm().describe()
                + (parsed.llmApiFlag ? "  (-LLMAPI: using .env / environment config)" : ""));

        switch (parsed.mode) {
            case "gui" -> new StudyGUI(agent).show();
            case "cli" -> {
                boolean interactive = parsed.inputFile == null;
                StudyCLI cli = new StudyCLI(
                        agent,
                        new java.util.Scanner(System.in),
                        System.out,
                        interactive);
                if (parsed.inputFile != null) {
                    cli.runFromFile(parsed.inputFile);
                } else {
                    cli.run();
                }
            }
            default -> {
                System.err.println("Unknown mode: " + parsed.mode);
                printHelp();
                System.exit(1);
            }
        }
    }

    static AgentController bootstrap() {
        AppConfig config = new AppConfig();
        DocumentStore store = new JsonDocumentStore(config.dataDir());
        ProgressTracker progress = new ProgressTracker(config.dataDir());
        LLMClient llm = LLMClientFactory.fromConfig(config);
        AiUsageLog.get().setLogFile(config.dataDir().resolve("ai-usage.log"));
        System.out.println("StudyBuddy data dir: " + config.dataDir());
        return new AgentController(store, llm, progress);
    }

    private static void printHelp() {
        System.out.println("""
                StudyBuddy Agent — EECS 3311
                Usage:
                  java -jar Agent.jar <gui|cli|help> [options]
                  java -jar Agent.jar -LLMAPI -input <task.txt>
                  java -jar Agent.jar cli --input <task.txt>

                Modes:
                  gui     Swing graphical interface
                  cli     Interactive terminal (default when -input is used)
                  help    Show this help

                Options:
                  -input <file>, --input <file>
                          Run CLI commands from a task file (non-interactive batch).
                          Supports $COURSE $MATERIAL $QUIZ placeholders.
                  -LLMAPI, --llm-api
                          Use the LLM configured in .env / environment
                          (LLM_PROVIDER + LLM_API_KEY). Same as normal startup.
                  -h, --help
                          Show this help

                Examples:
                  java -jar Agent.jar gui
                  java -jar Agent.jar cli
                  java -jar Agent.jar -LLMAPI -input examples/task.txt
                  mvn -q exec:java -Dexec.args="cli --input examples/task.txt"
                """);
    }

    /** Lightweight argv parser for mode + -input / -LLMAPI. */
    static final class ParsedArgs {
        final String mode;
        final Path inputFile;
        final boolean llmApiFlag;
        final boolean help;

        private ParsedArgs(String mode, Path inputFile, boolean llmApiFlag, boolean help) {
            this.mode = mode;
            this.inputFile = inputFile;
            this.llmApiFlag = llmApiFlag;
            this.help = help;
        }

        static ParsedArgs parse(String[] args) {
            List<String> tokens = new ArrayList<>();
            if (args != null) {
                for (String a : args) {
                    if (a != null && !a.isBlank()) {
                        tokens.add(a.trim());
                    }
                }
            }

            boolean help = false;
            boolean llmApi = false;
            Path input = null;
            String mode = null;

            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                String lower = t.toLowerCase();
                if (lower.equals("help") || lower.equals("-h") || lower.equals("--help")) {
                    help = true;
                    continue;
                }
                if (lower.equals("-llmapi") || lower.equals("--llm-api") || lower.equals("-llm-api")) {
                    llmApi = true;
                    continue;
                }
                if (lower.equals("-input") || lower.equals("--input")) {
                    if (i + 1 >= tokens.size()) {
                        throw new IllegalArgumentException("Missing path after " + t);
                    }
                    input = Path.of(tokens.get(++i));
                    if (!Files.isRegularFile(input)) {
                        throw new IllegalArgumentException("Input file not found: " + input.toAbsolutePath());
                    }
                    continue;
                }
                if (lower.equals("gui") || lower.equals("cli")) {
                    mode = lower;
                    continue;
                }
                // Unknown bare token — treat as input file path for convenience
                if (!t.startsWith("-") && Files.isRegularFile(Path.of(t)) && input == null) {
                    input = Path.of(t);
                    continue;
                }
                throw new IllegalArgumentException("Unknown argument: " + t);
            }

            if (mode == null) {
                mode = input != null ? "cli" : "gui";
            }
            return new ParsedArgs(mode, input, llmApi, help);
        }
    }
}
