package studymate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import studymate.agent.LearningAgent;
import studymate.agent.OpenAIAdapter;
import studymate.agent.RetrievalTool;
import studymate.agent.ToolRegistry;
import studymate.extraction.TextExtractor;
import studymate.model.AvailabilitySlot;
import studymate.model.Course;
import studymate.model.LearningArtifact;
import studymate.model.Material;
import studymate.model.StudyPlan;
import studymate.model.StudySession;
import studymate.planning.DeadlineStrategy;
import studymate.planning.SchedulingStrategy;
import studymate.planning.WeakTopicStrategy;
import studymate.repository.StudyRepository;
import studymate.service.MaterialService;

public class Main {

    public static void main(String[] args) {

        System.out.println(
                "=============================="
        );

        System.out.println(
                "          StudyMate"
        );

        System.out.println(
                "=============================="
        );

        StudyRepository repository =
                new StudyRepository();

        Course course =
                repository.getOrCreateCourse(
                        "EECS3311 Software Design"
                );

        TextExtractor extractor =
                new TextExtractor();

        MaterialService materialService =
                new MaterialService(
                        repository,
                        extractor
                );

        ToolRegistry toolRegistry =
                new ToolRegistry();

        toolRegistry.register(
                "retrieve",
                new RetrievalTool(repository)
        );

        OpenAIAdapter openAI =
                new OpenAIAdapter();

        LearningAgent learningAgent =
                new LearningAgent(
                        openAI,
                        toolRegistry,
                        repository
                );

        Scanner scanner =
                new Scanner(System.in);

        boolean running = true;

        while (running) {

            showMenu(course);

            String option =
                    scanner.nextLine()
                            .trim();

            switch (option) {

                case "1" ->
                        importMaterial(
                                scanner,
                                materialService,
                                course,
                                repository
                        );

                case "2" ->
                        summarise(
                                learningAgent,
                                course
                        );

                case "3" ->
                        extractConcepts(
                                learningAgent,
                                course
                        );

                case "4" ->
                        askQuestion(
                                scanner,
                                learningAgent,
                                course
                        );

                case "5" ->
                        generateStudyPlan(
                                scanner,
                                learningAgent,
                                course
                        );

                case "6" ->
                        showMaterials(
                                repository,
                                course
                        );

                case "0" -> {

                    running = false;

                    System.out.println();

                    System.out.println(
                            "StudyMate finished."
                    );
                }

                case "" -> {
                    // Ignore accidental blank input.
                }

                default -> {

                    System.out.println();

                    System.out.println(
                            "Invalid option."
                    );
                }
            }
        }

        scanner.close();
    }

    private static void showMenu(
            Course course) {

        System.out.println();

        System.out.println(
                "Course: "
                        + course.getName()
        );

        System.out.println();

        System.out.println(
                "1 - Import material"
        );

        System.out.println(
                "2 - Summarise materials"
        );

        System.out.println(
                "3 - Extract key concepts"
        );

        System.out.println(
                "4 - Ask a question"
        );

        System.out.println(
                "5 - Generate study plan"
        );

        System.out.println(
                "6 - Show saved materials"
        );

        System.out.println(
                "0 - Exit"
        );

        System.out.println();

        System.out.print(
                "Choose an option: "
        );
    }

    // =====================================================
    // F01
    // =====================================================

    private static void importMaterial(
            Scanner scanner,
            MaterialService materialService,
            Course course,
            StudyRepository repository) {

        System.out.println();

        System.out.print(
                "Enter PDF/TXT filename: "
        );

        String filePath =
                scanner.nextLine()
                        .trim();

        try {

            Material material =
                    materialService
                            .importMaterial(
                                    course.id(),
                                    filePath
                            );

            System.out.println();

            System.out.println(
                    "Import successful!"
            );

            System.out.println(
                    "Material: "
                            + material.getTitle()
            );

            System.out.println(
                    "Chunks: "
                            + material.chunks()
                            .size()
            );

        } catch (IOException e) {

            System.out.println();

            System.out.println(
                    "Import rejected: "
                            + e.getMessage()
            );
        }

        showMaterials(
                repository,
                course
        );
    }

    // =====================================================
    // F02
    // =====================================================

    private static void summarise(
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.println(
                "Generating AI summary..."
        );

        try {

            LearningArtifact summary =
                    learningAgent.summarise(
                            course.id()
                    );

            System.out.println();

            System.out.println(
                    "===== AI SUMMARY ====="
            );

            System.out.println();

            System.out.println(
                    summary.content()
            );

            System.out.println();

            System.out.println(
                    "Sources used: "
                            + summary
                            .getSourceIds()
                            .size()
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Summary failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F03
    // =====================================================

    private static void extractConcepts(
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.println(
                "Extracting key concepts..."
        );

        try {

            LearningArtifact concepts =
                    learningAgent
                            .extractConcepts(
                                    course.id()
                            );

            System.out.println();

            System.out.println(
                    "===== KEY CONCEPTS ====="
            );

            System.out.println();

            System.out.println(
                    concepts.content()
            );

            System.out.println();

            System.out.println(
                    "Sources used: "
                            + concepts
                            .getSourceIds()
                            .size()
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Concept extraction failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F04
    // =====================================================

    private static void askQuestion(
            Scanner scanner,
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.print(
                "Enter your question: "
        );

        String question =
                scanner.nextLine()
                        .trim();

        try {

            LearningArtifact answer =
                    learningAgent
                            .answerQuestion(
                                    course.id(),
                                    question
                            );

            System.out.println();

            System.out.println(
                    "===== AI ANSWER ====="
            );

            System.out.println();

            System.out.println(
                    answer.content()
            );

            System.out.println();

            System.out.println(
                    "Sources used: "
                            + answer
                            .getSourceIds()
                            .size()
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Question failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F05
    // =====================================================

    private static void generateStudyPlan(
            Scanner scanner,
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.println(
                "===== GENERATE STUDY PLAN ====="
        );

        try {

            System.out.println();

            System.out.print(
                    "Exam date (YYYY-MM-DD): "
            );

            LocalDate examDate =
                    LocalDate.parse(
                            scanner.nextLine()
                                    .trim()
                    );

            if (!examDate.isAfter(
                    LocalDate.now())) {

                System.out.println();

                System.out.println(
                        "Plan rejected: "
                                + "exam date must be "
                                + "in the future."
                );

                return;
            }

            System.out.print(
                    "Number of available study days: "
            );

            int numberOfDays =
                    Integer.parseInt(
                            scanner.nextLine()
                                    .trim()
                    );

            if (numberOfDays <= 0) {

                System.out.println();

                System.out.println(
                        "Plan rejected: "
                                + "availability must "
                                + "contain at least one day."
                );

                return;
            }

            List<AvailabilitySlot> availability =
                    new ArrayList<>();

            for (int i = 1;
                 i <= numberOfDays;
                 i++) {

                System.out.println();

                System.out.println(
                        "Availability "
                                + i
                                + ":"
                );

                System.out.print(
                        "Date (YYYY-MM-DD): "
                );

                LocalDate date =
                        LocalDate.parse(
                                scanner.nextLine()
                                        .trim()
                        );

                if (date.isBefore(
                        LocalDate.now())) {

                    System.out.println();

                    System.out.println(
                            "Plan rejected: "
                                    + "availability cannot "
                                    + "be in the past."
                    );

                    return;
                }

                if (!date.isBefore(
                        examDate)) {

                    System.out.println();

                    System.out.println(
                            "Plan rejected: "
                                    + "availability must be "
                                    + "before the exam date."
                    );

                    return;
                }

                System.out.print(
                        "Available minutes: "
                );

                int minutes =
                        Integer.parseInt(
                                scanner.nextLine()
                                        .trim()
                        );

                if (minutes <= 0) {

                    System.out.println();

                    System.out.println(
                            "Plan rejected: "
                                    + "available minutes "
                                    + "must be positive."
                    );

                    return;
                }

                availability.add(
                        new AvailabilitySlot(
                                date,
                                minutes
                        )
                );
            }

            System.out.println();

            System.out.println(
                    "Choose scheduling strategy:"
            );

            System.out.println(
                    "1 - Deadline"
            );

            System.out.println(
                    "2 - Weak Topic"
            );

            System.out.print(
                    "Strategy: "
            );

            String strategyChoice =
                    scanner.nextLine()
                            .trim();

            SchedulingStrategy strategy;

            if (strategyChoice.equals("1")) {

                strategy =
                        new DeadlineStrategy();

            } else if (strategyChoice.equals("2")) {

                strategy =
                        new WeakTopicStrategy();

            } else {

                System.out.println();

                System.out.println(
                        "Plan rejected: "
                                + "invalid strategy."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "Generating AI topic estimates..."
            );

            StudyPlan plan =
                    learningAgent
                            .generateStudyPlan(
                                    course.id(),
                                    examDate,
                                    availability,
                                    strategy
                            );

            printStudyPlan(
                    plan
            );

        } catch (DateTimeParseException e) {

            System.out.println();

            System.out.println(
                    "Plan rejected: "
                            + "date must use YYYY-MM-DD."
            );

        } catch (NumberFormatException e) {

            System.out.println();

            System.out.println(
                    "Plan rejected: "
                            + "please enter a valid number."
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Study plan failed: "
                            + e.getMessage()
            );
        }
    }

    private static void printStudyPlan(
            StudyPlan plan) {

        System.out.println();

        System.out.println(
                "===== STUDY PLAN ====="
        );

        System.out.println();

        System.out.println(
                "Exam date: "
                        + plan.getExamDate()
        );

        System.out.println(
                "Strategy: "
                        + plan.getStrategyName()
        );

        System.out.println();

        if (plan.getSessions()
                .isEmpty()) {

            System.out.println(
                    "No sessions could be scheduled."
            );

        } else {

            System.out.println(
                    "Scheduled sessions:"
            );

            for (StudySession session :
                    plan.getSessions()) {

                System.out.println(
                        "- "
                                + session.getDate()
                                + " | "
                                + session.getTopic()
                                + " | "
                                + session.getMinutes()
                                + " min"
                );
            }
        }

        System.out.println();

        if (plan.isFeasible()) {

            System.out.println(
                    "Plan status: FEASIBLE"
            );

            System.out.println(
                    "The study plan was saved."
            );

        } else {

            System.out.println(
                    "Plan status: "
                            + "INSUFFICIENT CAPACITY"
            );

            System.out.println(
                    "The study plan was NOT saved."
            );

            System.out.println();

            System.out.println(
                    "Unscheduled work:"
            );

            for (String topic :
                    plan.getUnscheduledTopics()) {

                System.out.println(
                        "- " + topic
                );
            }

            System.out.println();

            System.out.println(
                    "Add more study time "
                            + "or reduce the scope."
            );
        }
    }

    // =====================================================
    // MATERIAL LIST
    // =====================================================

    private static void showMaterials(
            StudyRepository repository,
            Course course) {

        List<Material> materials =
                repository
                        .getMaterialsByCourse(
                                course.id()
                        );

        System.out.println();

        System.out.println(
                "Saved materials for this course: "
                        + materials.size()
        );

        if (materials.isEmpty()) {

            System.out.println(
                    "No materials imported yet."
            );

            return;
        }

        for (Material material :
                materials) {

            System.out.println(
                    "- "
                            + material.getTitle()
                            + " | "
                            + material.chunks()
                            .size()
                            + " chunks"
            );
        }
    }
}