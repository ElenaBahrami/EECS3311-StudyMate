package studymate.agent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModelReply {

    public enum Type {
        TOOL_CALL,
        FINAL
    }

    private Type type;

    private String toolName;

    private Map<String, String> arguments;

    private String finalPayload;

    private ModelReply(
            Type type,
            String toolName,
            Map<String, String> arguments,
            String finalPayload) {

        this.type = type;
        this.toolName = toolName;

        if (arguments == null) {

            this.arguments =
                    new HashMap<>();

        } else {

            this.arguments =
                    new HashMap<>(
                            arguments
                    );
        }

        this.finalPayload =
                finalPayload;
    }

    public static ModelReply toolCall(
            String toolName,
            Map<String, String> arguments) {

        return new ModelReply(
                Type.TOOL_CALL,
                toolName,
                arguments,
                null
        );
    }

    public static ModelReply finalReply(
            String finalPayload) {

        return new ModelReply(
                Type.FINAL,
                null,
                Collections.emptyMap(),
                finalPayload
        );
    }

    public Type getType() {
        return type;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, String> getArguments() {
        return new HashMap<>(arguments);
    }

    public String getFinalPayload() {
        return finalPayload;
    }
}
