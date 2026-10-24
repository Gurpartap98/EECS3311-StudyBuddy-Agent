package studybuddy.agent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import studybuddy.tools.Result;
import studybuddy.tools.Tool;
import studybuddy.tools.ToolContext;

/**
 * Registers tools by name and dispatches {@link Tool#execute(ToolContext)}.
 */
public final class ToolManager {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        tools.put(tool.name(), tool);
    }

    public Result executeTool(String name, ToolContext ctx) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return Result.fail("Unknown tool: " + name);
        }
        return tool.execute(ctx);
    }

    public Collection<Tool> listTools() {
        return tools.values();
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
