package studybuddy;

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
        switch (mode) {
            case "cli" -> System.out.println(
                    "StudyBuddy CLI scaffold is not wired yet. "
                            + "Next commits add the domain model, AgentController, and StudyCLI.");
            case "gui" -> System.out.println(
                    "StudyBuddy GUI scaffold is not wired yet. "
                            + "Next commits add Swing StudyGUI on top of AgentController.");
            case "help", "-h", "--help" -> printHelp();
            default -> {
                System.err.println("Unknown mode: " + mode);
                printHelp();
                System.exit(1);
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
                StudyBuddy Agent — EECS 3311
                Usage: StudyBuddyApp <gui|cli|help>
                """);
    }
}
