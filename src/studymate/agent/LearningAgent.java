package studymate.agent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import studymate.model.AvailabilitySlot;
import studymate.model.LearningArtifact;
import studymate.model.StudyPlan;
import studymate.model.StudyTopic;
import studymate.planning.SchedulingStrategy;
import studymate.planning.StudyPlanner;
import studymate.repository.StudyRepository;

public class LearningAgent {

    private static final int MAX_STEPS = 6;

    private final LLMClient client;
    private final ToolRegistry tools;
    private final StudyRepository repository;
    private final StudyPlanner planner;

    public LearningAgent(
            LLMClient client,
            ToolRegistry tools,
            StudyRepository repository) {

        this(
                client,
                tools,
                repository,
                new StudyPlanner()
        );
    }

    public LearningAgent(
            LLMClient client,
            ToolRegistry tools,
            StudyRepository repository,
            StudyPlanner planner) {

        this.client = client;
        this.tools = tools;
        this.repository = repository;
        this.planner = planner;
    }

    // =====================================================
    // F02 - SUMMARISE MATERIALS
    // =====================================================

    public LearningArtifact summarise(
            UUID courseId)
            throws Exception {

        String instructions =
                """
                Create a concise study summary using only
                the supplied course evidence.

                Organize the summary into clear sections.

                Do not introduce facts that are not
                supported by the supplied evidence.

                Include source identifiers from the
                supplied [SOURCE ...] labels when making
                factual points.

                If the evidence is insufficient, say so.
                """;

        return generateArtifact(
                courseId,
                "SUMMARY",
                instructions,
                ""
        );
    }

    // =====================================================
    // F03 - EXTRACT KEY CONCEPTS
    // =====================================================

    public LearningArtifact extractConcepts(
            UUID courseId)
            throws Exception {

        String instructions =
                """
                Extract the most important concepts from
                the supplied course evidence.

                For each concept provide:

                1. Concept name
                2. Short definition
                3. Why the concept is important
                4. Relevant source identifier

                Focus only on concepts that are useful for
                studying the course material.

                Do not introduce concepts that are not
                supported by the supplied evidence.

                Use the supplied [SOURCE ...] identifiers
                when citing evidence.

                Avoid duplicates.

                Aim for approximately 8 to 12 key concepts.

                If the evidence is insufficient, say so.
                """;

        return generateArtifact(
                courseId,
                "CONCEPTS",
                instructions,
                ""
        );
    }

    // =====================================================
    // F04 - QUESTION AND ANSWER
    // =====================================================

    public LearningArtifact answerQuestion(
            UUID courseId,
            String question)
            throws Exception {

        if (question == null
                || question.isBlank()) {

            throw new IllegalArgumentException(
                    "Question cannot be empty."
            );
        }

        String instructions =
                """
                Answer the student's question using only
                the supplied course evidence.

                Student question:
                %s

                Give a clear and concise answer suitable
                for studying.

                Do not use outside knowledge.

                Do not invent information that is not
                supported by the supplied evidence.

                Include relevant [SOURCE ...] identifiers
                to show where the answer came from.

                If the supplied evidence does not contain
                enough information to answer the question,
                clearly say that the available course
                evidence is insufficient.
                """.formatted(question);

        return generateArtifact(
                courseId,
                "ANSWER",
                instructions,
                question
        );
    }

    // =====================================================
    // F05 - GENERATE PERSONALISED STUDY PLAN
    // =====================================================

    public StudyPlan generateStudyPlan(
            UUID courseId,
            LocalDate examDate,
            List<AvailabilitySlot> availability,
            SchedulingStrategy strategy)
            throws Exception {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (repository.findCourse(courseId)
                == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        if (examDate == null) {

            throw new IllegalArgumentException(
                    "Exam date is required."
            );
        }

        if (!examDate.isAfter(
                LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Exam date must be in the future."
            );
        }

        if (availability == null
                || availability.isEmpty()) {

            throw new IllegalArgumentException(
                    "Availability is required."
            );
        }

        if (strategy == null) {

            throw new IllegalArgumentException(
                    "Scheduling strategy is required."
            );
        }

        ToolResult retrieval =
                tools.execute(
                        "retrieve",
                        Map.of(
                                "courseId",
                                courseId.toString(),
                                "query",
                                ""
                        )
                );

        if (retrieval
                .getSourceIds()
                .isEmpty()) {

            throw new IllegalStateException(
                    "No course evidence is available."
            );
        }

        String instructions =
                """
                Identify the most important study topics
                from the supplied course evidence.

                For each topic estimate the amount of study
                time required and assign a priority.

                Return ONLY lines in exactly this format:

                TOPIC|topic name|estimated minutes|priority

                Rules:

                - Produce approximately 6 to 10 topics.
                - Estimated minutes must be a positive integer.
                - Use approximately 30 to 90 minutes per topic.
                - Priority must be an integer from 1 to 5.
                - Priority 5 means highest importance.
                - Use only topics supported by the supplied
                  course evidence.
                - Do not include headings.
                - Do not include explanations.
                - Do not include Markdown.
                - Do not include source identifiers.
                - Do not include any text before or after
                  the TOPIC lines.

                Example format:

                TOPIC|Adapter Pattern|45|5
                TOPIC|Observer Pattern|45|4
                """;

        ModelReply reply =
                client.generate(
                        retrieval.getData(),
                        instructions
                );

        if (reply.getType()
                != ModelReply.Type.FINAL) {

            throw new IllegalStateException(
                    "Model did not return study topics."
            );
        }

        List<StudyTopic> topics =
                parseStudyTopics(
                        reply.getFinalPayload()
                );

        if (topics.isEmpty()) {

            throw new IllegalStateException(
                    "No valid study topics were generated."
            );
        }

        StudyPlan plan =
                planner.createPlan(
                        courseId,
                        examDate,
                        topics,
                        availability,
                        strategy
                );

        /*
         * Stage 1 rule:
         * save only a feasible plan.
         *
         * If capacity is insufficient,
         * return the plan with its
         * unscheduled topics but do not
         * persist it.
         */
        if (plan.isFeasible()) {

            repository.savePlan(
                    plan
            );
        }

        return plan;
    }

    // =====================================================
    // PARSE AI TOPIC OUTPUT
    // =====================================================

    private List<StudyTopic> parseStudyTopics(
            String payload) {

        List<StudyTopic> topics =
                new ArrayList<>();

        if (payload == null
                || payload.isBlank()) {

            return topics;
        }

        String[] lines =
                payload.split("\\R");

        for (String rawLine :
                lines) {

            String line =
                    rawLine.trim();

            if (!line.startsWith(
                    "TOPIC|")) {

                continue;
            }

            String[] parts =
                    line.split(
                            "\\|",
                            -1
                    );

            if (parts.length != 4) {

                continue;
            }

            String name =
                    parts[1].trim();

            try {

                int minutes =
                        Integer.parseInt(
                                parts[2].trim()
                        );

                int priority =
                        Integer.parseInt(
                                parts[3].trim()
                        );

                if (name.isBlank()
                        || minutes <= 0
                        || priority < 1
                        || priority > 5) {

                    continue;
                }

                topics.add(
                        new StudyTopic(
                                name,
                                minutes,
                                priority
                        )
                );

            } catch (NumberFormatException e) {

                // Ignore malformed model lines.
            }
        }

        return topics;
    }

    // =====================================================
    // SHARED F02-F04 AGENT LOGIC
    // =====================================================

    private LearningArtifact generateArtifact(
            UUID courseId,
            String artifactKind,
            String instructions,
            String retrievalQuery)
            throws Exception {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (repository.findCourse(courseId)
                == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        StringBuilder context =
                new StringBuilder();

        List<UUID> usedSourceIds =
                new ArrayList<>();

        boolean retrievalUsed =
                false;

        for (int step = 0;
             step < MAX_STEPS;
             step++) {

            if (!retrievalUsed) {

                ToolResult retrieval =
                        tools.execute(
                                "retrieve",
                                Map.of(
                                        "courseId",
                                        courseId.toString(),
                                        "query",
                                        retrievalQuery
                                )
                        );

                if (retrieval
                        .getSourceIds()
                        .isEmpty()) {

                    if (!retrievalQuery.isBlank()) {

                        return new LearningArtifact(
                                courseId,
                                artifactKind,
                                "I could not find enough relevant "
                                        + "course evidence to answer "
                                        + "this question.",
                                List.of()
                        );
                    }

                    throw new IllegalStateException(
                            "No course evidence is available."
                    );
                }

                context.append(
                        retrieval.getData()
                );

                usedSourceIds.addAll(
                        retrieval.getSourceIds()
                );

                retrievalUsed = true;
            }

            ModelReply reply =
                    client.generate(
                            context.toString(),
                            instructions
                    );

            if (reply.getType()
                    == ModelReply.Type.FINAL) {

                String payload =
                        reply.getFinalPayload();

                if (payload == null
                        || payload.isBlank()) {

                    throw new IllegalStateException(
                            "Model returned an empty result."
                    );
                }

                return new LearningArtifact(
                        courseId,
                        artifactKind,
                        payload,
                        usedSourceIds
                );
            }

            if (reply.getType()
                    == ModelReply.Type.TOOL_CALL) {

                String toolName =
                        reply.getToolName();

                if (!tools.contains(toolName)) {

                    context.append(
                            "\nInvalid tool request: "
                                    + toolName
                                    + "\n"
                    );

                    continue;
                }

                ToolResult result =
                        tools.execute(
                                toolName,
                                reply.getArguments()
                        );

                context.append(
                        "\n"
                                + result.getData()
                                + "\n"
                );

                usedSourceIds.addAll(
                        result.getSourceIds()
                );
            }
        }

        throw new IllegalStateException(
                "Agent step limit reached "
                        + "without a valid result."
        );
    }
}