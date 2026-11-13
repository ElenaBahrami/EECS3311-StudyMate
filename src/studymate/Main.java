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
import studymate.controller.AgentController;
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

        // F10 Facade / Command invoker
        AgentController controller =
                new AgentController(
                        learningAgent,
                        repository
                );

        Scanner scanner =
                new Scanner(System.in);

        boolean running =
                true;

        while (running) {

            showMenu(
                    course
            );

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
                        adaptStudyPlan(
                                scanner,
                                controller,
                                repository,
                                course
                        );

                case "11" ->
                        confirmPlanRevision(
                                controller
                        );

                case "12" ->
                        rejectPlanRevision(
                                controller
                        );

                case "13" ->
                        undoPlanRevision(
                                controller
                        );

                case "14" ->
                        showLatestPlan(
                                repository,
                                course
                        );

                case "15" ->
                        showMaterials(
                                repository,
                                course
                        );

                case "0" -> {

                    running =
                            false;

                    System.out.println();

                    System.out.println(
                            "StudyMate finished."
                    );
                }

                case "" -> {
                    // Ignore blank input.
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
                "10 - Adapt study plan (preview)"
        );

        System.out.println(
                "11 - Confirm plan revision"
        );

        System.out.println(
                "12 - Reject plan revision"
        );

        System.out.println(
                "13 - Undo last plan revision"
        );

        System.out.println(
                "14 - Show latest study plan"
        );

        System.out.println(
                "15 - Show saved materials"
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
                            + material
                                    .chunks()
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
    // F02 - SUMMARY
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
    // F03 - CONCEPTS
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

            LocalDate examDate =
                    readExamDate(
                            scanner
                    );

            List<AvailabilitySlot> availability =
                    readAvailability(
                            scanner,
                            examDate
                    );

            SchedulingStrategy strategy =
                    readStrategy(
                            scanner
                    );

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

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Study plan failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F06 - FLASHCARDS
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

        try {

            int requestedCount =
                    Integer.parseInt(
                            scanner.nextLine()
                                    .trim()
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
                        + deck
                                .getCards()
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

        int number =
                1;

        for (Flashcard card :
                deck.getCards()) {

            System.out.println(
                    "Card "
                            + number
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

        System.out.println(
                "The flashcard deck was saved."
        );
    }

    // =====================================================
    // F07 - QUIZ GENERATION
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

        String difficulty =
                switch (difficultyChoice) {

                    case "1" -> "easy";
                    case "2" -> "medium";
                    case "3" -> "hard";

                    default ->
                            throw new IllegalArgumentException(
                                    "Invalid difficulty."
                            );
                };

        System.out.print(
                "Number of questions (1-20): "
        );

        try {

            int requestedCount =
                    Integer.parseInt(
                            scanner.nextLine()
                                    .trim()
                    );

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
                        + quiz
                                .getDifficulty()
                                .toUpperCase()
        );

        System.out.println(
                "Questions: "
                        + quiz
                                .getQuestions()
                                .size()
        );

        System.out.println();

        int number =
                1;

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
    // F08 - QUIZ GRADING
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

        UUID quizId;

        try {

            quizId =
                    UUID.fromString(
                            scanner.nextLine()
                                    .trim()
                    );

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Grading rejected: invalid Quiz ID."
            );

            return;
        }

        Quiz quiz =
                repository.findQuiz(
                        quizId
                );

        if (quiz == null) {

            System.out.println(
                    "Quiz was not found."
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
                        + quiz
                                .getDifficulty()
                                .toUpperCase()
        );

        System.out.println(
                "Questions: "
                        + quiz
                                .getQuestions()
                                .size()
        );

        System.out.println();

        System.out.print(
                "Attempt ID "
                        + "(press Enter to create one): "
        );

        String attemptText =
                scanner.nextLine()
                        .trim();

        UUID attemptId;

        if (attemptText.isBlank()) {

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
                                attemptText
                        );

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "Invalid Attempt ID."
                );

                return;
            }
        }

        QuizAttempt previous =
                repository.findAttempt(
                        attemptId
                );

        if (previous != null) {

            if (!previous
                    .getQuizId()
                    .equals(quizId)) {

                System.out.println(
                        "Attempt ID belongs "
                                + "to another quiz."
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
                    previous,
                    learningAgent,
                    false
            );

            return;
        }

        List<Integer> answers =
                new ArrayList<>();

        boolean skipped =
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
                            answers.add(0);

                    case "B" ->
                            answers.add(1);

                    case "C" ->
                            answers.add(2);

                    case "D" ->
                            answers.add(3);

                    case "S" -> {

                        answers.add(
                                null
                        );

                        skipped =
                                true;
                    }

                    default -> {

                        System.out.println(
                                "Please enter A, B, C, D, or S."
                        );

                        continue;
                    }
                }

                break;
            }
        }

        if (skipped) {

            System.out.println();

            System.out.println(
                    "Skipped questions receive zero credit."
            );

            System.out.print(
                    "Submit with skipped answers? (Y/N): "
            );

            if (!scanner.nextLine()
                    .trim()
                    .equalsIgnoreCase("Y")) {

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
                            answers
                    );

            printQuizResult(
                    quiz,
                    attempt,
                    learningAgent,
                    true
            );

        } catch (Exception e) {

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

        boolean hasWrong =
                results.stream()
                        .anyMatch(
                                result ->
                                        !result.isCorrect()
                        );

        if (hasWrong
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

            System.out.println(
                    "Result: "
                            + (result.isCorrect()
                            ? "CORRECT"
                            : "INCORRECT")
            );

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

                } else {

                    System.out.println(
                            newSubmission
                                    ? "Explanation: "
                                    + question.getExplanation()
                                    : "Stored explanation: "
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

            System.out.println(
                    "===== WEAK TOPICS ====="
            );

            System.out.println();

            List<TopicProgress> weakTopics =
                    summary.getWeakTopics();

            if (weakTopics.isEmpty()) {

                System.out.println(
                        "No topics currently meet "
                                + "the weak-topic rule."
                );

            } else {

                int rank =
                        1;

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

            System.out.println();

            System.out.println(
                    "===== INSUFFICIENT DATA ====="
            );

            System.out.println();

            List<TopicProgress> insufficient =
                    summary.getInsufficientDataTopics();

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
    // F10 - ADAPT / PREVIEW
    // =====================================================

    private static void adaptStudyPlan(
            Scanner scanner,
            AgentController controller,
            StudyRepository repository,
            Course course) {

        System.out.println();

        System.out.println(
                "===== ADAPT STUDY PLAN ====="
        );

        StudyPlan current =
                repository.findLatestPlanByCourse(
                        course.id()
                );

        if (current == null) {

            System.out.println();

            System.out.println(
                    "No saved study plan exists."
            );

            System.out.println(
                    "Generate a study plan first "
                            + "using option 5."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Current saved plan:"
        );

        printStudyPlan(
                current
        );

        try {

            System.out.println();

            System.out.println(
                    "Enter the revised constraints."
            );

            LocalDate examDate =
                    readExamDate(
                            scanner
                    );

            List<AvailabilitySlot> availability =
                    readAvailability(
                            scanner,
                            examDate
                    );

            SchedulingStrategy strategy =
                    readStrategy(
                            scanner
                    );

            System.out.println();

            System.out.println(
                    "Generating revision preview..."
            );

            StudyPlan preview =
                    controller.previewRevision(
                            course.id(),
                            examDate,
                            availability,
                            strategy
                    );

            System.out.println();

            System.out.println(
                    "===== REVISION PREVIEW ====="
            );

            printStudyPlan(
                    preview
            );

            if (!preview.isFeasible()) {

                System.out.println();

                System.out.println(
                        "The revision is not feasible."
                );

                System.out.println(
                        "Nothing was saved."
                );

                return;
            }

            System.out.println();

            System.out.println(
                    "PREVIEW ONLY."
            );

            System.out.println(
                    "The saved plan has NOT changed."
            );

            System.out.println(
                    "Choose option 11 to confirm "
                            + "or option 12 to reject."
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Adaptation failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F10 - CONFIRM
    // =====================================================

    private static void confirmPlanRevision(
            AgentController controller) {

        System.out.println();

        System.out.println(
                "===== CONFIRM PLAN REVISION ====="
        );

        if (!controller.hasPendingRevision()) {

            System.out.println();

            System.out.println(
                    "There is no pending revision."
            );

            return;
        }

        try {

            StudyPlan saved =
                    controller.confirmRevision();

            System.out.println();

            System.out.println(
                    "Revision confirmed and saved."
            );

            printStudyPlan(
                    saved
            );

            System.out.println();

            System.out.println(
                    "One-level undo is now available."
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Confirmation failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // F10 - REJECT
    // =====================================================

    private static void rejectPlanRevision(
            AgentController controller) {

        System.out.println();

        System.out.println(
                "===== REJECT PLAN REVISION ====="
        );

        if (!controller.hasPendingRevision()) {

            System.out.println();

            System.out.println(
                    "There is no pending revision."
            );

            return;
        }

        controller.rejectRevision();

        System.out.println();

        System.out.println(
                "Revision rejected."
        );

        System.out.println(
                "The saved plan was not changed."
        );
    }

    // =====================================================
    // F10 - UNDO
    // =====================================================

    private static void undoPlanRevision(
            AgentController controller) {

        System.out.println();

        System.out.println(
                "===== UNDO LAST PLAN REVISION ====="
        );

        if (!controller.canUndo()) {

            System.out.println();

            System.out.println(
                    "There is no confirmed revision "
                            + "available to undo."
            );

            return;
        }

        try {

            StudyPlan restored =
                    controller.undoLastRevision();

            System.out.println();

            System.out.println(
                    "Revision successfully undone."
            );

            System.out.println(
                    "The previous schedule was restored "
                            + "as a new version."
            );

            printStudyPlan(
                    restored
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Undo failed: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // SHOW LATEST PLAN
    // =====================================================

    private static void showLatestPlan(
            StudyRepository repository,
            Course course) {

        System.out.println();

        System.out.println(
                "===== LATEST STUDY PLAN ====="
        );

        StudyPlan plan =
                repository.findLatestPlanByCourse(
                        course.id()
                );

        if (plan == null) {

            System.out.println();

            System.out.println(
                    "No saved study plan exists."
            );

            return;
        }

        printStudyPlan(
                plan
        );
    }

    // =====================================================
    // SHARED PLAN INPUT
    // =====================================================

    private static LocalDate readExamDate(
            Scanner scanner) {

        System.out.print(
                "Exam date (YYYY-MM-DD): "
        );

        try {

            LocalDate date =
                    LocalDate.parse(
                            scanner.nextLine()
                                    .trim()
                    );

            if (!date.isAfter(
                    LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Exam date must be in the future."
                );
            }

            return date;

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Date must use YYYY-MM-DD."
            );
        }
    }

    private static List<AvailabilitySlot>
            readAvailability(
                    Scanner scanner,
                    LocalDate examDate) {

        System.out.print(
                "Number of available study days: "
        );

        int numberOfDays;

        try {

            numberOfDays =
                    Integer.parseInt(
                            scanner.nextLine()
                                    .trim()
                    );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Please enter a valid number."
            );
        }

        if (numberOfDays <= 0) {

            throw new IllegalArgumentException(
                    "At least one study day is required."
            );
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

            LocalDate date;

            try {

                date =
                        LocalDate.parse(
                                scanner.nextLine()
                                        .trim()
                        );

            } catch (DateTimeParseException e) {

                throw new IllegalArgumentException(
                        "Date must use YYYY-MM-DD."
                );
            }

            if (date.isBefore(
                    LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Availability cannot be in the past."
                );
            }

            if (!date.isBefore(
                    examDate)) {

                throw new IllegalArgumentException(
                        "Availability must be "
                                + "before the exam date."
                );
            }

            System.out.print(
                    "Available minutes: "
            );

            int minutes;

            try {

                minutes =
                        Integer.parseInt(
                                scanner.nextLine()
                                        .trim()
                        );

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Available minutes "
                                + "must be a number."
                );
            }

            if (minutes <= 0) {

                throw new IllegalArgumentException(
                        "Available minutes "
                                + "must be positive."
                );
            }

            availability.add(
                    new AvailabilitySlot(
                            date,
                            minutes
                    )
            );
        }

        return availability;
    }

    private static SchedulingStrategy readStrategy(
            Scanner scanner) {

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

        String choice =
                scanner.nextLine()
                        .trim();

        return switch (choice) {

            case "1" ->
                    new DeadlineStrategy();

            case "2" ->
                    new WeakTopicStrategy();

            default ->
                    throw new IllegalArgumentException(
                            "Invalid scheduling strategy."
                    );
        };
    }

    // =====================================================
    // PRINT STUDY PLAN
    // =====================================================

    private static void printStudyPlan(
            StudyPlan plan) {

        System.out.println();

        System.out.println(
                "Plan ID: "
                        + plan.getId()
        );

        System.out.println(
                "Version: "
                        + plan.getVersion()
        );

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

        } else {

            System.out.println(
                    "Plan status: "
                            + "INSUFFICIENT CAPACITY"
            );

            if (!plan.getUnscheduledTopics()
                    .isEmpty()) {

                System.out.println();

                System.out.println(
                        "Unscheduled work:"
                );

                for (String topic :
                        plan.getUnscheduledTopics()) {

                    System.out.println(
                            "- "
                                    + topic
                    );
                }
            }
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
                            + material
                                    .chunks()
                                    .size()
                            + " chunks"
            );
        }
    }
}