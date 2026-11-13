package studymate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LearningArtifact {

    private UUID id;
    private UUID courseId;
    private String kind;
    private String content;
    private List<UUID> sourceIds;

    public LearningArtifact(
            UUID courseId,
            String kind,
            String content,
            List<UUID> sourceIds) {

        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.kind = kind;
        this.content = content;
        this.sourceIds =
                new ArrayList<>(sourceIds);
    }

    public LearningArtifact(
            UUID id,
            UUID courseId,
            String kind,
            String content,
            List<UUID> sourceIds) {

        this.id = id;
        this.courseId = courseId;
        this.kind = kind;
        this.content = content;
        this.sourceIds =
                new ArrayList<>(sourceIds);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getKind() {
        return kind;
    }

    public String content() {
        return content;
    }

    public List<UUID> getSourceIds() {
        return new ArrayList<>(sourceIds);
    }
}
