package studymate.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import studymate.model.Chunk;
import studymate.model.Material;
import studymate.repository.StudyRepository;

public class RetrievalTool
        implements Tool {

    private final StudyRepository repository;

    public RetrievalTool(
            StudyRepository repository) {

        this.repository = repository;
    }

    @Override
    public ToolResult execute(
            Map<String, String> arguments) {

        String courseIdText =
                arguments.get("courseId");

        String query =
                arguments.getOrDefault(
                        "query",
                        ""
                );

        if (courseIdText == null
                || courseIdText.isBlank()) {

            return new ToolResult(
                    "Missing course ID.",
                    List.of()
            );
        }

        UUID courseId;

        try {

            courseId =
                    UUID.fromString(
                            courseIdText
                    );

        } catch (IllegalArgumentException e) {

            return new ToolResult(
                    "Invalid course ID.",
                    List.of()
            );
        }

        List<Material> materials =
                repository
                        .getMaterialsByCourse(
                                courseId
                        );

        if (materials.isEmpty()) {

            return new ToolResult(
                    "No materials are available "
                            + "for this course.",
                    List.of()
            );
        }

        String normalizedQuery =
                query.trim()
                        .toLowerCase();

        StringBuilder evidence =
                new StringBuilder();

        List<UUID> sourceIds =
                new ArrayList<>();

        for (Material material :
                materials) {

            for (Chunk chunk :
                    material.chunks()) {

                boolean matches;

                if (normalizedQuery.isBlank()) {

                    matches = true;

                } else {

                    matches =
                            chunk.getText()
                                    .toLowerCase()
                                    .contains(
                                            normalizedQuery
                                    );
                }

                if (matches) {

                    sourceIds.add(
                            chunk.getId()
                    );

                    evidence.append(
                            "[SOURCE "
                                    + chunk.getId()
                                    + " | "
                                    + material.getTitle()
                                    + " | "
                                    + chunk.sourceRef()
                                    + "]"
                    );

                    evidence.append("\n");

                    evidence.append(
                            chunk.getText()
                    );

                    evidence.append(
                            "\n\n"
                    );
                }
            }
        }

        if (sourceIds.isEmpty()) {

            return new ToolResult(
                    "No matching evidence found.",
                    List.of()
            );
        }

        return new ToolResult(
                evidence.toString(),
                sourceIds
        );
    }
}