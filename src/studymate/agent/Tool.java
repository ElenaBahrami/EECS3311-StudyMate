package studymate.agent;

import java.util.Map;

public interface Tool {

    ToolResult execute(
            Map<String, String> arguments
    );
}
