package studymate.agent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import studymate.model.AvailabilitySlot;
import studymate.model.Flashcard;
import studymate.model.FlashcardDeck;
import studymate.model.LearningArtifact;
import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
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
    // F05 - GENERATE STUDY PLAN
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

        if (retrieval.getSourceIds()
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

        if (plan.isFeasible()) {

            repository.savePlan(
                    plan
            );
        }

        return plan;
    }

    // =====================================================
    // F06 - GENERATE FLASHCARDS
    // =====================================================

    public FlashcardDeck generateFlashcards(
            UUID courseId,
            String topic,
            int requestedCount)
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

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Topic cannot be empty."
            );
        }

        if (requestedCount < 1
                || requestedCount > 30) {

            throw new IllegalArgumentException(
                    "Flashcard count must be between 1 and 30."
            );
        }

        ToolResult retrieval =
                tools.execute(
                        "retrieve",
                        Map.of(
                                "courseId",
                                courseId.toString(),
                                "query",
                                topic
                        )
                );

        if (retrieval.getSourceIds()
                .isEmpty()) {

            return new FlashcardDeck(
                    courseId,
                    topic,
                    requestedCount,
                    List.of()
            );
        }

        String instructions =
                """
                Generate study flashcards using only the
                supplied course evidence.

                Requested topic:
                %s

                Requested number of cards:
                %d

                Return ONLY lines in exactly this format:

                CARD|source UUID|question|answer

                Rules:

                - Generate no more than the requested count.
                - Every card must be supported by the evidence.
                - The source UUID must exactly match one of
                  the supplied [SOURCE ...] identifiers.
                - Questions must be distinct.
                - Answers must be concise and factual.
                - Do not use outside knowledge.
                - If the evidence supports fewer cards,
                  return fewer cards.
                - Do not include headings.
                - Do not include Markdown.
                - Do not include any other text.

                Example:

                CARD|123e4567-e89b-12d3-a456-426614174000|What is an adapter?|It translates between incompatible interfaces.
                """.formatted(
                        topic,
                        requestedCount
                );

        ModelReply reply =
                client.generate(
                        retrieval.getData(),
                        instructions
                );

        if (reply.getType()
                != ModelReply.Type.FINAL) {

            throw new IllegalStateException(
                    "Model did not return flashcards."
            );
        }

        List<Flashcard> cards =
                parseFlashcards(
                        reply.getFinalPayload(),
                        retrieval.getSourceIds(),
                        requestedCount
                );

        FlashcardDeck deck =
                new FlashcardDeck(
                        courseId,
                        topic,
                        requestedCount,
                        cards
                );

        if (!deck.getCards()
                .isEmpty()) {

            repository.saveFlashcardDeck(
                    deck
            );
        }

        return deck;
    }

    // =====================================================
    // F07 - GENERATE MULTIPLE-CHOICE QUIZ
    // =====================================================

    public Quiz generateQuiz(
            UUID courseId,
            String topic,
            String difficulty,
            int requestedCount)
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

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz topic cannot be empty."
            );
        }

        if (difficulty == null
                || difficulty.isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz difficulty is required."
            );
        }

        String normalizedDifficulty =
                difficulty.trim()
                        .toLowerCase();

        if (!normalizedDifficulty.equals("easy")
                && !normalizedDifficulty.equals("medium")
                && !normalizedDifficulty.equals("hard")) {

            throw new IllegalArgumentException(
                    "Difficulty must be Easy, Medium, or Hard."
            );
        }

        if (requestedCount < 1
                || requestedCount > 20) {

            throw new IllegalArgumentException(
                    "Quiz count must be between 1 and 20."
            );
        }

        ToolResult retrieval =
                tools.execute(
                        "retrieve",
                        Map.of(
                                "courseId",
                                courseId.toString(),
                                "query",
                                topic
                        )
                );

        if (retrieval.getSourceIds()
                .isEmpty()) {

            throw new IllegalStateException(
                    "No relevant course evidence was found "
                            + "for this quiz topic."
            );
        }

        String instructions =
                buildQuizInstructions(
                        topic,
                        normalizedDifficulty,
                        requestedCount
                );

        ModelReply firstReply =
                client.generate(
                        retrieval.getData(),
                        instructions
                );

        if (firstReply.getType()
                != ModelReply.Type.FINAL) {

            throw new IllegalStateException(
                    "Model did not return quiz questions."
            );
        }

        List<Question> questions =
                parseQuizQuestions(
                        firstReply.getFinalPayload(),
                        retrieval.getSourceIds(),
                        requestedCount
                );

        // One repair attempt.
        if (questions.size()
                != requestedCount) {

            String repairInstructions =
                    buildQuizRepairInstructions(
                            topic,
                            normalizedDifficulty,
                            requestedCount,
                            firstReply.getFinalPayload()
                    );

            ModelReply repairReply =
                    client.generate(
                            retrieval.getData(),
                            repairInstructions
                    );

            if (repairReply.getType()
                    == ModelReply.Type.FINAL) {

                questions =
                        parseQuizQuestions(
                                repairReply.getFinalPayload(),
                                retrieval.getSourceIds(),
                                requestedCount
                        );
            }
        }

        if (questions.size()
                != requestedCount) {

            throw new IllegalStateException(
                    "Quiz generation failed validation "
                            + "after one repair attempt. "
                            + "No incomplete quiz was saved."
            );
        }

        Quiz quiz =
                new Quiz(
                        courseId,
                        topic,
                        normalizedDifficulty,
                        requestedCount,
                        questions
                );

        repository.saveQuiz(
                quiz
        );

        return quiz;
    }

    private String buildQuizInstructions(
            String topic,
            String difficulty,
            int requestedCount) {

        return """
                Generate a multiple-choice quiz using only
                the supplied course evidence.

                Topic:
                %s

                Difficulty:
                %s

                Number of questions:
                %d

                Return EXACTLY %d valid questions.

                Return ONLY one line per question using
                exactly this format:

                QUESTION|source UUID|question|option A|option B|option C|option D|correct letter|explanation

                Rules:

                - Each question must have exactly four choices.
                - Correct letter must be A, B, C, or D.
                - Exactly one option must be correct.
                - Questions must be distinct.
                - The four options within a question must
                  also be distinct.
                - Every question must be supported by the
                  supplied course evidence.
                - Source UUID must exactly match one of the
                  supplied [SOURCE ...] identifiers.
                - Explanation must briefly explain why the
                  answer is correct using the evidence.
                - Do not use outside knowledge.
                - Do not include Markdown.
                - Do not include headings.
                - Do not number the questions.
                - Do not include text before or after the
                  QUESTION lines.
                - Do not use the | character inside a
                  question, option, or explanation.
                """.formatted(
                topic,
                difficulty,
                requestedCount,
                requestedCount
        );
    }

    private String buildQuizRepairInstructions(
            String topic,
            String difficulty,
            int requestedCount,
            String previousOutput) {

        return """
                The previous quiz output failed validation.

                Generate the entire quiz again from scratch.

                Topic:
                %s

                Difficulty:
                %s

                Required number of questions:
                %d

                You MUST return EXACTLY %d valid questions.

                Return ONLY one line per question:

                QUESTION|source UUID|question|option A|option B|option C|option D|correct letter|explanation

                Validation rules:

                - Exactly four nonempty choices per question.
                - Correct letter must be A, B, C, or D.
                - Exactly one option is correct.
                - No duplicate questions.
                - No duplicate options within a question.
                - Every source UUID must exactly match one
                  of the supplied [SOURCE ...] identifiers.
                - Every question and explanation must be
                  supported by the supplied evidence.
                - No outside knowledge.
                - No headings.
                - No Markdown.
                - No numbering.
                - No extra text.
                - Do not use | inside question text,
                  choices, or explanations.

                Previous invalid output:

                %s
                """.formatted(
                topic,
                difficulty,
                requestedCount,
                requestedCount,
                previousOutput == null
                        ? ""
                        : previousOutput
        );
    }

    // =====================================================
    // F08 - EXPLAIN QUIZ MISTAKES
    // =====================================================

    public Map<UUID, String> explainMistakes(
            Quiz quiz,
            QuizAttempt attempt)
            throws Exception {

        if (quiz == null) {

            throw new IllegalArgumentException(
                    "Quiz is required."
            );
        }

        if (attempt == null) {

            throw new IllegalArgumentException(
                    "Quiz attempt is required."
            );
        }

        if (!quiz.getId()
                .equals(attempt.getQuizId())) {

            throw new IllegalArgumentException(
                    "Quiz attempt does not belong "
                            + "to this quiz."
            );
        }

        List<Question> questions =
                quiz.getQuestions();

        List<QuestionResult> results =
                attempt.getResults();

        if (questions.size()
                != results.size()) {

            throw new IllegalArgumentException(
                    "Quiz and attempt results do not match."
            );
        }

        List<Question> wrongQuestions =
                new ArrayList<>();

        for (int i = 0;
             i < results.size();
             i++) {

            if (!results.get(i)
                    .isCorrect()) {

                wrongQuestions.add(
                        questions.get(i)
                );
            }
        }

        if (wrongQuestions.isEmpty()) {

            return Map.of();
        }

        ToolResult retrieval =
                tools.execute(
                        "retrieve",
                        Map.of(
                                "courseId",
                                quiz.getCourseId()
                                        .toString(),
                                "query",
                                quiz.getTopic()
                        )
                );

        if (retrieval.getSourceIds()
                .isEmpty()) {

            return Map.of();
        }

        StringBuilder mistakes =
                new StringBuilder();

        for (Question question :
                wrongQuestions) {

            mistakes.append(
                    "QUESTION_ID: "
                            + question.getId()
                            + "\n"
            );

            mistakes.append(
                    "QUESTION: "
                            + question.getText()
                            + "\n"
            );

            mistakes.append(
                    "CORRECT ANSWER: "
                            + question
                            .getCorrectOptionLetter()
                            + ". "
                            + question
                            .getOptions()
                            .get(
                                    question
                                    .getCorrectOptionIndex()
                            )
                            + "\n"
            );

            mistakes.append(
                    "SOURCE_ID: "
                            + question.getSourceId()
                            + "\n\n"
            );
        }

        String instructions =
                """
                Explain the student's incorrect quiz answers
                using only the supplied course evidence.

                The incorrect questions are:

                %s

                Return ONLY one line for each incorrect
                question using exactly this format:

                FEEDBACK|question UUID|explanation

                Rules:

                - Explain why the stored correct answer is correct.
                - Keep each explanation concise and useful for studying.
                - Use only the supplied course evidence.
                - Do not change or challenge the stored answer key.
                - Do not calculate or modify the quiz score.
                - Do not use outside knowledge.
                - Only return feedback for the listed question IDs.
                - Do not include Markdown.
                - Do not include headings.
                - Do not include any other text.
                - Do not use the | character inside an explanation.
                """.formatted(
                        mistakes.toString()
                );

        ModelReply reply =
                client.generate(
                        retrieval.getData(),
                        instructions
                );

        if (reply.getType()
                != ModelReply.Type.FINAL) {

            return Map.of();
        }

        return parseQuizFeedback(
                reply.getFinalPayload(),
                wrongQuestions
        );
    }

    private Map<UUID, String> parseQuizFeedback(
            String payload,
            List<Question> wrongQuestions) {

        Map<UUID, String> feedback =
                new LinkedHashMap<>();

        if (payload == null
                || payload.isBlank()) {

            return feedback;
        }

        Set<UUID> allowedQuestionIds =
                new HashSet<>();

        for (Question question :
                wrongQuestions) {

            allowedQuestionIds.add(
                    question.getId()
            );
        }

        String[] lines =
                payload.split("\\R");

        for (String rawLine :
                lines) {

            String line =
                    rawLine.trim();

            if (!line.startsWith(
                    "FEEDBACK|")) {

                continue;
            }

            String[] parts =
                    line.split(
                            "\\|",
                            3
                    );

            if (parts.length != 3) {

                continue;
            }

            UUID questionId;

            try {

                questionId =
                        UUID.fromString(
                                parts[1].trim()
                        );

            } catch (IllegalArgumentException e) {

                continue;
            }

            if (!allowedQuestionIds
                    .contains(questionId)) {

                continue;
            }

            String explanation =
                    parts[2].trim();

            if (explanation.isBlank()) {

                continue;
            }

            feedback.put(
                    questionId,
                    explanation
            );
        }

        return feedback;
    }

    // =====================================================
    // F05 PARSING
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

                // Ignore malformed AI output.
            }
        }

        return topics;
    }

    // =====================================================
    // F06 PARSING + VALIDATION
    // =====================================================

    private List<Flashcard> parseFlashcards(
            String payload,
            List<UUID> allowedSourceIds,
            int requestedCount) {

        List<Flashcard> cards =
                new ArrayList<>();

        if (payload == null
                || payload.isBlank()) {

            return cards;
        }

        Set<UUID> allowedSources =
                new HashSet<>(
                        allowedSourceIds
                );

        Set<String> usedQuestions =
                new HashSet<>();

        String[] lines =
                payload.split("\\R");

        for (String rawLine :
                lines) {

            if (cards.size()
                    >= requestedCount) {

                break;
            }

            String line =
                    rawLine.trim();

            if (!line.startsWith(
                    "CARD|")) {

                continue;
            }

            String[] parts =
                    line.split(
                            "\\|",
                            4
                    );

            if (parts.length != 4) {

                continue;
            }

            String sourceText =
                    parts[1].trim();

            String question =
                    parts[2].trim();

            String answer =
                    parts[3].trim();

            if (question.isBlank()
                    || answer.isBlank()) {

                continue;
            }

            UUID sourceId;

            try {

                sourceId =
                        UUID.fromString(
                                sourceText
                        );

            } catch (IllegalArgumentException e) {

                continue;
            }

            if (!allowedSources.contains(
                    sourceId)) {

                continue;
            }

            String normalizedQuestion =
                    question
                            .toLowerCase()
                            .trim();

            if (usedQuestions.contains(
                    normalizedQuestion)) {

                continue;
            }

            usedQuestions.add(
                    normalizedQuestion
            );

            cards.add(
                    new Flashcard(
                            question,
                            answer,
                            sourceId
                    )
            );
        }

        return cards;
    }

    // =====================================================
    // F07 PARSING + VALIDATION
    // =====================================================

    private List<Question> parseQuizQuestions(
            String payload,
            List<UUID> allowedSourceIds,
            int requestedCount) {

        List<Question> questions =
                new ArrayList<>();

        if (payload == null
                || payload.isBlank()) {

            return questions;
        }

        Set<UUID> allowedSources =
                new HashSet<>(
                        allowedSourceIds
                );

        Set<String> usedQuestions =
                new HashSet<>();

        String[] lines =
                payload.split("\\R");

        for (String rawLine :
                lines) {

            if (questions.size()
                    >= requestedCount) {

                break;
            }

            String line =
                    rawLine.trim();

            if (!line.startsWith(
                    "QUESTION|")) {

                continue;
            }

            String[] parts =
                    line.split(
                            "\\|",
                            -1
                    );

            if (parts.length != 9) {

                continue;
            }

            String sourceText =
                    parts[1].trim();

            String questionText =
                    parts[2].trim();

            String optionA =
                    parts[3].trim();

            String optionB =
                    parts[4].trim();

            String optionC =
                    parts[5].trim();

            String optionD =
                    parts[6].trim();

            String answerLetter =
                    parts[7].trim()
                            .toUpperCase();

            String explanation =
                    parts[8].trim();

            if (questionText.isBlank()
                    || optionA.isBlank()
                    || optionB.isBlank()
                    || optionC.isBlank()
                    || optionD.isBlank()
                    || explanation.isBlank()) {

                continue;
            }

            UUID sourceId;

            try {

                sourceId =
                        UUID.fromString(
                                sourceText
                        );

            } catch (IllegalArgumentException e) {

                continue;
            }

            if (!allowedSources.contains(
                    sourceId)) {

                continue;
            }

            int correctOptionIndex =
                    switch (answerLetter) {

                        case "A" -> 0;
                        case "B" -> 1;
                        case "C" -> 2;
                        case "D" -> 3;

                        default -> -1;
                    };

            if (correctOptionIndex == -1) {

                continue;
            }

            List<String> options =
                    List.of(
                            optionA,
                            optionB,
                            optionC,
                            optionD
                    );

            Set<String> normalizedOptions =
                    new HashSet<>();

            boolean duplicateOption =
                    false;

            for (String option :
                    options) {

                String normalized =
                        option
                                .toLowerCase()
                                .trim();

                if (!normalizedOptions.add(
                        normalized)) {

                    duplicateOption =
                            true;

                    break;
                }
            }

            if (duplicateOption) {

                continue;
            }

            String normalizedQuestion =
                    questionText
                            .toLowerCase()
                            .trim();

            if (!usedQuestions.add(
                    normalizedQuestion)) {

                continue;
            }

            try {

                Question question =
                        new Question(
                                questionText,
                                options,
                                correctOptionIndex,
                                explanation,
                                sourceId
                        );

                questions.add(
                        question
                );

            } catch (IllegalArgumentException e) {

                // Reject malformed generated question.
            }
        }

        return questions;
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

                retrievalUsed =
                        true;
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

                if (!tools.contains(
                        toolName)) {

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