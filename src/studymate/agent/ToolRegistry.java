package studymate.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, Tool> tools;

    public ToolRegistry() {

        tools =
                new HashMap<>();
    }

    public void register(
            String name,
            Tool tool) {

        tools.put(
                name,
                tool
        );
    }

    public boolean contains(
            String name) {

        return tools.containsKey(
                name
        );
    }

    public ToolResult execute(
            String name,
            Map<String, String> arguments) {

        Tool tool =
                tools.get(name);

        if (tool == null) {

            return new ToolResult(
                    "Unknown or disallowed tool: "
                            + name,
                    List.of()
            );
        }

        return tool.execute(
                arguments
        );
    }
}