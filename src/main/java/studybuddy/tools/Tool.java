package studybuddy.tools;

/**
 * Agent capability contract. Every concrete tool implements {@link #execute(ToolContext)}.
 * Helper methods such as retrieve(...) or explain(...) are called from execute, not instead of it.
 */
public interface Tool {

    /** Stable tool name used by {@code ToolManager} and the planner. */
    String name();

    /** Execute this tool using the shared context and typed parameters. */
    Result execute(ToolContext ctx);
}
