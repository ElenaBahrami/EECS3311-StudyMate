package studymate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import studymate.agent.LearningAgent;
import studymate.agent.OpenAIAdapter;
import studymate.agent.RetrievalTool;
import studymate.agent.ToolRegistry;
import studymate.extraction.TextExtractor;
import studymate.model.AvailabilitySlot;
import studymate.model.Course;
import studymate.model.Flashcard;
import studymate.model.FlashcardDeck;
import studymate.model.LearningArtifact;
import studymate.model.Material;
import studymate.model.ProgressSummary;
import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
import studymate.model.StudyPlan;
import studymate.model.StudySession;
import studymate.model.TopicProgress;
import studymate.planning.DeadlineStrategy;
import studymate.planning.SchedulingStrategy;
import studymate.planning.WeakTopicStrategy;
import studymate.progress.ProgressTracker;
import studymate.repository.StudyRepository;
import studymate.service.MaterialService;
import studymate.service.QuizService;

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

        // =================================================
        // F09 - PROGRESS TRACKER / OBSERVER
        // =================================================

        ProgressTracker progressTracker =
                new ProgressTracker(
                        repository
                );

        progressTracker.addListener(
                summary -> {

                    System.out.println();

                    System.out.println(
                            "Progress updated after "
                                    + "committed quiz attempt."
                    );
                }
        );

        QuizService quizService =
                new QuizService(
                        repository,
                        progressTracker
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
                        generateFlashcards(
                                scanner,
                                learningAgent,
                                course
                        );

                case "7" ->
                        generateQuiz(
                                scanner,
                                learningAgent,
                                course
                        );

                case "8" ->
                        gradeQuiz(
                                scanner,
                                repository,
                                quizService,
                                learningAgent
                        );

                case "9" ->
                        viewProgress(
                                progressTracker,
                                course
                        );

                case "10" ->
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
                "6 - Generate flashcards"
        );

        System.out.println(
                "7 - Generate quiz"
        );

        System.out.println(
                "8 - Grade quiz"
        );

        System.out.println(
                "9 - View progress"
        );

        System.out.println(
                "10 - Show saved materials"
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
    // F01 - IMPORT MATERIAL
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
    // F02 - SUMMARISE MATERIALS
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
    // F03 - EXTRACT KEY CONCEPTS
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
    // F04 - QUESTION AND ANSWER
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
    // F05 - GENERATE STUDY PLAN
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
    // F06 - GENERATE FLASHCARDS
    // =====================================================

    private static void generateFlashcards(
            Scanner scanner,
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.println(
                "===== GENERATE FLASHCARDS ====="
        );

        System.out.println();

        System.out.print(
                "Topic: "
        );

        String topic =
                scanner.nextLine()
                        .trim();

        if (topic.isBlank()) {

            System.out.println();

            System.out.println(
                    "Flashcard generation rejected: "
                            + "topic cannot be empty."
            );

            return;
        }

        System.out.print(
                "Number of flashcards (1-30): "
        );

        String countText =
                scanner.nextLine()
                        .trim();

        try {

            int requestedCount =
                    Integer.parseInt(
                            countText
                    );

            if (requestedCount < 1
                    || requestedCount > 30) {

                System.out.println();

                System.out.println(
                        "Flashcard generation rejected: "
                                + "count must be between "
                                + "1 and 30."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "Generating grounded flashcards..."
            );

            FlashcardDeck deck =
                    learningAgent
                            .generateFlashcards(
                                    course.id(),
                                    topic,
                                    requestedCount
                            );

            printFlashcardDeck(
                    deck
            );

        } catch (NumberFormatException e) {

            System.out.println();

            System.out.println(
                    "Flashcard generation rejected: "
                            + "please enter a valid number."
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Flashcard generation failed: "
                            + e.getMessage()
            );
        }
    }

    private static void printFlashcardDeck(
            FlashcardDeck deck) {

        System.out.println();

        System.out.println(
                "===== FLASHCARD DECK ====="
        );

        System.out.println();

        System.out.println(
                "Topic: "
                        + deck.getTopic()
        );

        System.out.println(
                "Requested cards: "
                        + deck.getRequestedCount()
        );

        System.out.println(
                "Generated cards: "
                        + deck.getCards()
                                .size()
        );

        System.out.println();

        if (deck.getCards()
                .isEmpty()) {

            System.out.println(
                    "No supported flashcards "
                            + "could be generated."
            );

            System.out.println(
                    "The deck was NOT saved."
            );

            return;
        }

        int number = 1;

        for (Flashcard card :
                deck.getCards()) {

            System.out.println(
                    "Card " + number
            );

            System.out.println(
                    "Q: "
                            + card.getQuestion()
            );

            System.out.println(
                    "A: "
                            + card.getAnswer()
            );

            System.out.println(
                    "Source: "
                            + card.getSourceId()
            );

            System.out.println();

            number++;
        }

        if (deck.isReducedDeck()) {

            System.out.println(
                    "Note: The available course evidence "
                            + "supported fewer cards than "
                            + "requested."
            );

            System.out.println();
        }

        System.out.println(
                "The flashcard deck was saved."
        );
    }

    // =====================================================
    // F07 - GENERATE QUIZ
    // =====================================================

    private static void generateQuiz(
            Scanner scanner,
            LearningAgent learningAgent,
            Course course) {

        System.out.println();

        System.out.println(
                "===== GENERATE QUIZ ====="
        );

        System.out.println();

        System.out.print(
                "Topic: "
        );

        String topic =
                scanner.nextLine()
                        .trim();

        if (topic.isBlank()) {

            System.out.println();

            System.out.println(
                    "Quiz generation rejected: "
                            + "topic cannot be empty."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Choose difficulty:"
        );

        System.out.println(
                "1 - Easy"
        );

        System.out.println(
                "2 - Medium"
        );

        System.out.println(
                "3 - Hard"
        );

        System.out.print(
                "Difficulty: "
        );

        String difficultyChoice =
                scanner.nextLine()
                        .trim();

        String difficulty;

        switch (difficultyChoice) {

            case "1" ->
                    difficulty = "easy";

            case "2" ->
                    difficulty = "medium";

            case "3" ->
                    difficulty = "hard";

            default -> {

                System.out.println();

                System.out.println(
                        "Quiz generation rejected: "
                                + "invalid difficulty."
                );

                return;
            }
        }

        System.out.print(
                "Number of questions (1-20): "
        );

        try {

            int requestedCount =
                    Integer.parseInt(
                            scanner.nextLine()
                                    .trim()
                    );

            if (requestedCount < 1
                    || requestedCount > 20) {

                System.out.println();

                System.out.println(
                        "Quiz generation rejected: "
                                + "count must be between "
                                + "1 and 20."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "Generating grounded quiz..."
            );

            Quiz quiz =
                    learningAgent
                            .generateQuiz(
                                    course.id(),
                                    topic,
                                    difficulty,
                                    requestedCount
                            );

            printQuiz(
                    quiz
            );

        } catch (NumberFormatException e) {

            System.out.println();

            System.out.println(
                    "Quiz generation rejected: "
                            + "please enter a valid number."
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Quiz generation failed: "
                            + e.getMessage()
            );
        }
    }

    private static void printQuiz(
            Quiz quiz) {

        System.out.println();

        System.out.println(
                "===== QUIZ ====="
        );

        System.out.println();

        System.out.println(
                "Quiz ID: "
                        + quiz.getId()
        );

        System.out.println(
                "Topic: "
                        + quiz.getTopic()
        );

        System.out.println(
                "Difficulty: "
                        + quiz.getDifficulty()
                                .toUpperCase()
        );

        System.out.println(
                "Questions: "
                        + quiz.getQuestions()
                                .size()
        );

        System.out.println();

        int number = 1;

        for (Question question :
                quiz.getQuestions()) {

            List<String> options =
                    question.getOptions();

            System.out.println(
                    number
                            + ". "
                            + question.getText()
            );

            System.out.println(
                    "A. "
                            + options.get(0)
            );

            System.out.println(
                    "B. "
                            + options.get(1)
            );

            System.out.println(
                    "C. "
                            + options.get(2)
            );

            System.out.println(
                    "D. "
                            + options.get(3)
            );

            System.out.println();

            number++;
        }

        System.out.println(
                "The quiz was validated and saved."
        );

        System.out.println(
                "Answer keys are hidden until grading."
        );
    }

    // =====================================================
    // F08 - GRADE QUIZ
    // =====================================================

    private static void gradeQuiz(
            Scanner scanner,
            StudyRepository repository,
            QuizService quizService,
            LearningAgent learningAgent) {

        System.out.println();

        System.out.println(
                "===== GRADE QUIZ ====="
        );

        System.out.println();

        System.out.print(
                "Quiz ID: "
        );

        String quizIdText =
                scanner.nextLine()
                        .trim();

        UUID quizId;

        try {

            quizId =
                    UUID.fromString(
                            quizIdText
                    );

        } catch (IllegalArgumentException e) {

            System.out.println();

            System.out.println(
                    "Grading rejected: "
                            + "invalid Quiz ID."
            );

            return;
        }

        Quiz quiz =
                repository.findQuiz(
                        quizId
                );

        if (quiz == null) {

            System.out.println();

            System.out.println(
                    "Grading rejected: "
                            + "quiz was not found."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Topic: "
                        + quiz.getTopic()
        );

        System.out.println(
                "Difficulty: "
                        + quiz.getDifficulty()
                                .toUpperCase()
        );

        System.out.println(
                "Questions: "
                        + quiz.getQuestions()
                                .size()
        );

        System.out.println();

        System.out.print(
                "Attempt ID "
                        + "(press Enter to create one): "
        );

        String attemptIdText =
                scanner.nextLine()
                        .trim();

        UUID attemptId;

        if (attemptIdText.isBlank()) {

            attemptId =
                    UUID.randomUUID();

            System.out.println(
                    "Generated Attempt ID: "
                            + attemptId
            );

        } else {

            try {

                attemptId =
                        UUID.fromString(
                                attemptIdText
                        );

            } catch (IllegalArgumentException e) {

                System.out.println();

                System.out.println(
                        "Grading rejected: "
                                + "invalid Attempt ID."
                );

                return;
            }
        }

        QuizAttempt previousAttempt =
                repository.findAttempt(
                        attemptId
                );

        if (previousAttempt != null) {

            if (!previousAttempt
                    .getQuizId()
                    .equals(quizId)) {

                System.out.println();

                System.out.println(
                        "Grading rejected: "
                                + "this Attempt ID already "
                                + "belongs to another quiz."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "This Attempt ID was already submitted."
            );

            System.out.println(
                    "Returning the existing result "
                            + "without grading again."
            );

            printQuizResult(
                    quiz,
                    previousAttempt,
                    learningAgent,
                    false
            );

            return;
        }

        List<Integer> selectedAnswers =
                new ArrayList<>();

        boolean hasSkippedAnswers =
                false;

        List<Question> questions =
                quiz.getQuestions();

        for (int i = 0;
             i < questions.size();
             i++) {

            Question question =
                    questions.get(i);

            List<String> options =
                    question.getOptions();

            System.out.println();

            System.out.println(
                    (i + 1)
                            + ". "
                            + question.getText()
            );

            System.out.println(
                    "A. "
                            + options.get(0)
            );

            System.out.println(
                    "B. "
                            + options.get(1)
            );

            System.out.println(
                    "C. "
                            + options.get(2)
            );

            System.out.println(
                    "D. "
                            + options.get(3)
            );

            while (true) {

                System.out.print(
                        "Your answer "
                                + "(A/B/C/D or S to skip): "
                );

                String answer =
                        scanner.nextLine()
                                .trim()
                                .toUpperCase();

                switch (answer) {

                    case "A" ->
                            selectedAnswers.add(0);

                    case "B" ->
                            selectedAnswers.add(1);

                    case "C" ->
                            selectedAnswers.add(2);

                    case "D" ->
                            selectedAnswers.add(3);

                    case "S" -> {

                        selectedAnswers.add(
                                null
                        );

                        hasSkippedAnswers =
                                true;
                    }

                    default -> {

                        System.out.println(
                                "Please enter A, B, C, D, "
                                        + "or S."
                        );

                        continue;
                    }
                }

                break;
            }
        }

        if (hasSkippedAnswers) {

            System.out.println();

            System.out.println(
                    "Skipped questions receive zero credit."
            );

            System.out.print(
                    "Submit with skipped answers? "
                            + "(Y/N): "
            );

            String confirmation =
                    scanner.nextLine()
                            .trim()
                            .toUpperCase();

            if (!confirmation.equals("Y")) {

                System.out.println();

                System.out.println(
                        "Submission cancelled."
                );

                return;
            }
        }

        try {

            QuizAttempt attempt =
                    quizService.grade(
                            quizId,
                            attemptId,
                            selectedAnswers
                    );

            printQuizResult(
                    quiz,
                    attempt,
                    learningAgent,
                    true
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Grading failed: "
                            + e.getMessage()
            );
        }
    }

    private static void printQuizResult(
            Quiz quiz,
            QuizAttempt attempt,
            LearningAgent learningAgent,
            boolean newSubmission) {

        System.out.println();

        System.out.println(
                "===== QUIZ RESULT ====="
        );

        System.out.println();

        System.out.println(
                "Attempt ID: "
                        + attempt.getId()
        );

        System.out.println(
                "Quiz ID: "
                        + attempt.getQuizId()
        );

        System.out.println(
                "Correct: "
                        + attempt.getCorrectCount()
                        + " / "
                        + attempt.getGradedCount()
        );

        System.out.printf(
                "Score: %.2f%%%n",
                attempt.getScorePercent()
        );

        System.out.println();

        List<Question> questions =
                quiz.getQuestions();

        List<QuestionResult> results =
                attempt.getResults();

        Map<UUID, String> feedback =
                Map.of();

        boolean hasWrongAnswer =
                false;

        for (QuestionResult result :
                results) {

            if (!result.isCorrect()) {

                hasWrongAnswer =
                        true;

                break;
            }
        }

        if (hasWrongAnswer
                && newSubmission) {

            System.out.println(
                    "Generating explanations "
                            + "for incorrect answers..."
            );

            try {

                feedback =
                        learningAgent
                                .explainMistakes(
                                        quiz,
                                        attempt
                                );

            } catch (Exception e) {

                System.out.println(
                        "AI explanations are unavailable, "
                                + "but your score is saved."
                );
            }

            System.out.println();
        }

        for (int i = 0;
             i < results.size();
             i++) {

            Question question =
                    questions.get(i);

            QuestionResult result =
                    results.get(i);

            System.out.println(
                    "Question "
                            + (i + 1)
                            + ":"
            );

            if (result.isCorrect()) {

                System.out.println(
                        "Result: CORRECT"
                );

            } else {

                System.out.println(
                        "Result: INCORRECT"
                );
            }

            if (result.isSkipped()) {

                System.out.println(
                        "Your answer: SKIPPED"
                );

            } else {

                System.out.println(
                        "Your answer: "
                                + optionLetter(
                                        result
                                                .getSelectedOptionIndex()
                                )
                );
            }

            System.out.println(
                    "Correct answer: "
                            + optionLetter(
                                    result
                                            .getCorrectOptionIndex()
                            )
                            + ". "
                            + question
                                    .getOptions()
                                    .get(
                                            result
                                                    .getCorrectOptionIndex()
                                    )
            );

            if (!result.isCorrect()) {

                String explanation =
                        feedback.get(
                                question.getId()
                        );

                if (explanation != null
                        && !explanation.isBlank()) {

                    System.out.println(
                            "Explanation: "
                                    + explanation
                    );

                } else if (newSubmission) {

                    System.out.println(
                            "Explanation: "
                                    + question.getExplanation()
                    );

                } else {

                    System.out.println(
                            "Stored explanation: "
                                    + question.getExplanation()
                    );
                }
            }

            System.out.println();
        }

        if (newSubmission) {

            System.out.println(
                    "The quiz attempt was saved."
            );

        } else {

            System.out.println(
                    "Existing quiz attempt loaded."
            );

            System.out.println(
                    "No duplicate attempt was saved."
            );
        }

        System.out.println(
                "The score was calculated "
                        + "deterministically from "
                        + "the stored answer key."
        );
    }

    private static String optionLetter(
            Integer optionIndex) {

        if (optionIndex == null) {

            return "SKIPPED";
        }

        return switch (optionIndex) {

            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";

            default -> "?";
        };
    }

    // =====================================================
    // F09 - VIEW PROGRESS
    // =====================================================

    private static void viewProgress(
            ProgressTracker progressTracker,
            Course course) {

        System.out.println();

        System.out.println(
                "===== COURSE PROGRESS ====="
        );

        try {

            ProgressSummary summary =
                    progressTracker.analyse(
                            course.id()
                    );

            if (summary.isEmpty()) {

                System.out.println();

                System.out.println(
                        "No quiz attempts have been "
                                + "recorded yet."
                );

                System.out.println(
                        "No progress judgement was made."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "Topic results:"
            );

            System.out.println();

            for (TopicProgress topic :
                    summary.getTopics()) {

                System.out.println(
                        "Topic: "
                                + topic.getTopic()
                );

                System.out.println(
                        "Correct: "
                                + topic.getCorrectCount()
                                + " / "
                                + topic.getGradedCount()
                );

                System.out.printf(
                        "Accuracy: %.2f%%%n",
                        topic.getAccuracyPercent()
                );

                System.out.println(
                        "Evidence count: "
                                + topic.getGradedCount()
                );

                System.out.println(
                        "Status: "
                                + topic.getStatus()
                );

                System.out.println();
            }

            List<TopicProgress> weakTopics =
                    summary.getWeakTopics();

            System.out.println(
                    "===== WEAK TOPICS ====="
            );

            System.out.println();

            if (weakTopics.isEmpty()) {

                System.out.println(
                        "No topics currently meet "
                                + "the weak-topic rule."
                );

            } else {

                int rank = 1;

                for (TopicProgress topic :
                        weakTopics) {

                    System.out.printf(
                            "%d. %s - %.2f%% (%d graded)%n",
                            rank,
                            topic.getTopic(),
                            topic.getAccuracyPercent(),
                            topic.getGradedCount()
                    );

                    rank++;
                }
            }

            List<TopicProgress> insufficient =
                    summary
                            .getInsufficientDataTopics();

            System.out.println();

            System.out.println(
                    "===== INSUFFICIENT DATA ====="
            );

            System.out.println();

            if (insufficient.isEmpty()) {

                System.out.println(
                        "No topics have insufficient data."
                );

            } else {

                for (TopicProgress topic :
                        insufficient) {

                    System.out.println(
                            "- "
                                    + topic.getTopic()
                                    + " | "
                                    + topic.getGradedCount()
                                    + " graded observation(s)"
                    );
                }
            }

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Progress could not be loaded: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // SHOW MATERIALS
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