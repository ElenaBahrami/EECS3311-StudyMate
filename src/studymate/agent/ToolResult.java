package studymate.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ToolResult {

    private String data;
    private List<UUID> sourceIds;

    public ToolResult(
            String data,
            List<UUID> sourceIds) {

        this.data = data;
        this.sourceIds =
                new ArrayList<>(sourceIds);
    }

    public String getData() {
        return data;
    }

    public List<UUID> getSourceIds() {
        return new ArrayList<>(sourceIds);
    }
}
