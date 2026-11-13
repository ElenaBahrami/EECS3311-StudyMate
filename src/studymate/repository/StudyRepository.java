package studymate.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import studymate.model.Chunk;
import studymate.model.Course;
import studymate.model.Flashcard;
import studymate.model.FlashcardDeck;
import studymate.model.Material;
import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
import studymate.model.StudyPlan;
import studymate.model.StudySession;

public class StudyRepository {

    private static final String DATABASE_URL =
            "jdbc:sqlite:studymate.db";

    private Connection connection;

    public StudyRepository() {

        try {

            connection =
                    DriverManager.getConnection(
                            DATABASE_URL
                    );

            try (Statement statement =
                         connection.createStatement()) {

                statement.execute(
                        "PRAGMA foreign_keys = ON"
                );
            }

            createTables();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to connect to database.",
                    e
            );
        }
    }

    // =====================================================
    // DATABASE TABLES
    // =====================================================

    private void createTables()
            throws SQLException {

        String createCoursesTable =
                """
                CREATE TABLE IF NOT EXISTS courses (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE
                );
                """;

        String createMaterialsTable =
                """
                CREATE TABLE IF NOT EXISTS materials (
                    id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    hash TEXT NOT NULL UNIQUE,

                    FOREIGN KEY(course_id)
                        REFERENCES courses(id)
                        ON DELETE CASCADE
                );
                """;

        String createChunksTable =
                """
                CREATE TABLE IF NOT EXISTS chunks (
                    id TEXT PRIMARY KEY,
                    material_id TEXT NOT NULL,
                    page INTEGER NOT NULL,
                    text TEXT NOT NULL,

                    FOREIGN KEY(material_id)
                        REFERENCES materials(id)
                        ON DELETE CASCADE
                );
                """;

        String createStudyPlansTable =
                """
                CREATE TABLE IF NOT EXISTS study_plans (
                    id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    exam_date TEXT NOT NULL,
                    strategy_name TEXT NOT NULL,

                    FOREIGN KEY(course_id)
                        REFERENCES courses(id)
                        ON DELETE CASCADE
                );
                """;

        String createStudySessionsTable =
                """
                CREATE TABLE IF NOT EXISTS study_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plan_id TEXT NOT NULL,
                    session_order INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    minutes INTEGER NOT NULL,

                    FOREIGN KEY(plan_id)
                        REFERENCES study_plans(id)
                        ON DELETE CASCADE
                );
                """;

        String createFlashcardDecksTable =
                """
                CREATE TABLE IF NOT EXISTS flashcard_decks (
                    id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    requested_count INTEGER NOT NULL,

                    FOREIGN KEY(course_id)
                        REFERENCES courses(id)
                        ON DELETE CASCADE
                );
                """;

        String createFlashcardsTable =
                """
                CREATE TABLE IF NOT EXISTS flashcards (
                    id TEXT PRIMARY KEY,
                    deck_id TEXT NOT NULL,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    source_id TEXT NOT NULL,

                    FOREIGN KEY(deck_id)
                        REFERENCES flashcard_decks(id)
                        ON DELETE CASCADE
                );
                """;

        String createQuizzesTable =
                """
                CREATE TABLE IF NOT EXISTS quizzes (
                    id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    requested_count INTEGER NOT NULL,

                    FOREIGN KEY(course_id)
                        REFERENCES courses(id)
                        ON DELETE CASCADE
                );
                """;

        String createQuizQuestionsTable =
                """
                CREATE TABLE IF NOT EXISTS quiz_questions (
                    id TEXT PRIMARY KEY,
                    quiz_id TEXT NOT NULL,
                    question_order INTEGER NOT NULL,
                    question_text TEXT NOT NULL,
                    option_a TEXT NOT NULL,
                    option_b TEXT NOT NULL,
                    option_c TEXT NOT NULL,
                    option_d TEXT NOT NULL,
                    correct_option_index INTEGER NOT NULL,
                    explanation TEXT NOT NULL,
                    source_id TEXT NOT NULL,

                    FOREIGN KEY(quiz_id)
                        REFERENCES quizzes(id)
                        ON DELETE CASCADE
                );
                """;

        String createQuizAttemptsTable =
                """
                CREATE TABLE IF NOT EXISTS quiz_attempts (
                    id TEXT PRIMARY KEY,
                    quiz_id TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    correct_count INTEGER NOT NULL,
                    graded_count INTEGER NOT NULL,
                    score_percent REAL NOT NULL,

                    FOREIGN KEY(quiz_id)
                        REFERENCES quizzes(id)
                        ON DELETE CASCADE
                );
                """;

        String createQuizAttemptResultsTable =
                """
                CREATE TABLE IF NOT EXISTS quiz_attempt_results (
                    attempt_id TEXT NOT NULL,
                    question_id TEXT NOT NULL,
                    result_order INTEGER NOT NULL,
                    selected_option_index INTEGER,
                    correct_option_index INTEGER NOT NULL,
                    is_correct INTEGER NOT NULL,

                    PRIMARY KEY(attempt_id, question_id),

                    FOREIGN KEY(attempt_id)
                        REFERENCES quiz_attempts(id)
                        ON DELETE CASCADE,

                    FOREIGN KEY(question_id)
                        REFERENCES quiz_questions(id)
                        ON DELETE CASCADE
                );
                """;

        try (Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    createCoursesTable
            );

            statement.execute(
                    createMaterialsTable
            );

            statement.execute(
                    createChunksTable
            );

            statement.execute(
                    createStudyPlansTable
            );

            statement.execute(
                    createStudySessionsTable
            );

            statement.execute(
                    createFlashcardDecksTable
            );

            statement.execute(
                    createFlashcardsTable
            );

            statement.execute(
                    createQuizzesTable
            );

            statement.execute(
                    createQuizQuestionsTable
            );

            statement.execute(
                    createQuizAttemptsTable
            );

            statement.execute(
                    createQuizAttemptResultsTable
            );
        }
    }

    // =====================================================
    // COURSES
    // =====================================================

    public Course saveCourse(
            Course course) {

        String sql =
                """
                INSERT INTO courses (id, name)
                VALUES (?, ?);
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    course.id().toString()
            );

            statement.setString(
                    2,
                    course.getName()
            );

            statement.executeUpdate();

            return course;

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to save course.",
                    e
            );
        }
    }

    public Course findCourse(
            UUID courseId) {

        String sql =
                """
                SELECT id, name
                FROM courses
                WHERE id = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    courseId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                return new Course(
                        UUID.fromString(
                                result.getString("id")
                        ),
                        result.getString("name")
                );
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to find course.",
                    e
            );
        }

        return null;
    }

    public Course findCourseByName(
            String name) {

        String sql =
                """
                SELECT id, name
                FROM courses
                WHERE name = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    name
            );

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                return new Course(
                        UUID.fromString(
                                result.getString("id")
                        ),
                        result.getString("name")
                );
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to find course.",
                    e
            );
        }

        return null;
    }

    public Course getOrCreateCourse(
            String name) {

        Course existing =
                findCourseByName(name);

        if (existing != null) {
            return existing;
        }

        Course course =
                new Course(name);

        return saveCourse(course);
    }

    // =====================================================
    // MATERIALS
    // =====================================================

    public Material saveMaterial(
            Material material) {

        String materialSql =
                """
                INSERT INTO materials
                (id, course_id, title, hash)
                VALUES (?, ?, ?, ?);
                """;

        String chunkSql =
                """
                INSERT INTO chunks
                (id, material_id, page, text)
                VALUES (?, ?, ?, ?);
                """;

        try {

            connection.setAutoCommit(false);

            try (PreparedStatement materialStatement =
                         connection.prepareStatement(
                                 materialSql
                         )) {

                materialStatement.setString(
                        1,
                        material.getId()
                                .toString()
                );

                materialStatement.setString(
                        2,
                        material.getCourseId()
                                .toString()
                );

                materialStatement.setString(
                        3,
                        material.getTitle()
                );

                materialStatement.setString(
                        4,
                        material.getHash()
                );

                materialStatement.executeUpdate();
            }

            try (PreparedStatement chunkStatement =
                         connection.prepareStatement(
                                 chunkSql
                         )) {

                for (Chunk chunk :
                        material.chunks()) {

                    chunkStatement.setString(
                            1,
                            chunk.getId()
                                    .toString()
                    );

                    chunkStatement.setString(
                            2,
                            material.getId()
                                    .toString()
                    );

                    chunkStatement.setInt(
                            3,
                            chunk.getPage()
                    );

                    chunkStatement.setString(
                            4,
                            chunk.getText()
                    );

                    chunkStatement.addBatch();
                }

                chunkStatement.executeBatch();
            }

            connection.commit();

            return material;

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            throw new RuntimeException(
                    "Unable to save material.",
                    e
            );

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Material findMaterial(
            UUID materialId) {

        String sql =
                """
                SELECT id, course_id, title, hash
                FROM materials
                WHERE id = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    materialId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                Material material =
                        createMaterialFromResult(
                                result
                        );

                loadChunks(material);

                return material;
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to find material.",
                    e
            );
        }

        return null;
    }

    public Material findMaterialByHash(
            String hash) {

        String sql =
                """
                SELECT id, course_id, title, hash
                FROM materials
                WHERE hash = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    hash
            );

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                Material material =
                        createMaterialFromResult(
                                result
                        );

                loadChunks(material);

                return material;
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to search material.",
                    e
            );
        }

        return null;
    }

    public List<Material>
            getMaterialsByCourse(
                    UUID courseId) {

        List<Material> materials =
                new ArrayList<>();

        String sql =
                """
                SELECT id, course_id, title, hash
                FROM materials
                WHERE course_id = ?
                ORDER BY title;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    courseId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                Material material =
                        createMaterialFromResult(
                                result
                        );

                loadChunks(material);

                materials.add(material);
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load course materials.",
                    e
            );
        }

        return materials;
    }

    public List<Material>
            getAllMaterials() {

        List<Material> materials =
                new ArrayList<>();

        String sql =
                """
                SELECT id, course_id, title, hash
                FROM materials
                ORDER BY title;
                """;

        try (Statement statement =
                     connection.createStatement();

             ResultSet result =
                     statement.executeQuery(sql)) {

            while (result.next()) {

                Material material =
                        createMaterialFromResult(
                                result
                        );

                loadChunks(material);

                materials.add(material);
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load materials.",
                    e
            );
        }

        return materials;
    }

    private Material createMaterialFromResult(
            ResultSet result)
            throws SQLException {

        UUID id =
                UUID.fromString(
                        result.getString("id")
                );

        UUID courseId =
                UUID.fromString(
                        result.getString(
                                "course_id"
                        )
                );

        String title =
                result.getString(
                        "title"
                );

        String hash =
                result.getString(
                        "hash"
                );

        return new Material(
                id,
                courseId,
                title,
                hash
        );
    }

    private void loadChunks(
            Material material)
            throws SQLException {

        String sql =
                """
                SELECT id, page, text
                FROM chunks
                WHERE material_id = ?
                ORDER BY page;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    material.getId()
                            .toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                Chunk chunk =
                        new Chunk(
                                UUID.fromString(
                                        result.getString(
                                                "id"
                                        )
                                ),
                                result.getInt(
                                        "page"
                                ),
                                result.getString(
                                        "text"
                                )
                        );

                material.addChunk(chunk);
            }
        }
    }

    // =====================================================
    // F05 - STUDY PLANS
    // =====================================================

    public StudyPlan savePlan(
            StudyPlan plan) {

        if (plan == null) {

            throw new IllegalArgumentException(
                    "Study plan is required."
            );
        }

        if (!plan.isFeasible()) {

            throw new IllegalArgumentException(
                    "Cannot save an infeasible study plan."
            );
        }

        if (findCourse(
                plan.getCourseId()) == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        String planSql =
                """
                INSERT INTO study_plans
                (id, course_id, exam_date, strategy_name)
                VALUES (?, ?, ?, ?);
                """;

        String sessionSql =
                """
                INSERT INTO study_sessions
                (plan_id, session_order, date, topic, minutes)
                VALUES (?, ?, ?, ?, ?);
                """;

        try {

            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 planSql
                         )) {

                statement.setString(
                        1,
                        plan.getId()
                                .toString()
                );

                statement.setString(
                        2,
                        plan.getCourseId()
                                .toString()
                );

                statement.setString(
                        3,
                        plan.getExamDate()
                                .toString()
                );

                statement.setString(
                        4,
                        plan.getStrategyName()
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 sessionSql
                         )) {

                int order = 1;

                for (StudySession session :
                        plan.getSessions()) {

                    statement.setString(
                            1,
                            plan.getId()
                                    .toString()
                    );

                    statement.setInt(
                            2,
                            order
                    );

                    statement.setString(
                            3,
                            session.getDate()
                                    .toString()
                    );

                    statement.setString(
                            4,
                            session.getTopic()
                    );

                    statement.setInt(
                            5,
                            session.getMinutes()
                    );

                    statement.addBatch();

                    order++;
                }

                statement.executeBatch();
            }

            connection.commit();

            return plan;

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            throw new RuntimeException(
                    "Unable to save study plan.",
                    e
            );

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    // =====================================================
    // F06 - FLASHCARDS
    // =====================================================

    public FlashcardDeck saveFlashcardDeck(
            FlashcardDeck deck) {

        if (deck == null) {

            throw new IllegalArgumentException(
                    "Flashcard deck is required."
            );
        }

        if (deck.getCards()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot save an empty flashcard deck."
            );
        }

        if (findCourse(
                deck.getCourseId()) == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        String deckSql =
                """
                INSERT INTO flashcard_decks
                (id, course_id, topic, requested_count)
                VALUES (?, ?, ?, ?);
                """;

        String cardSql =
                """
                INSERT INTO flashcards
                (id, deck_id, question, answer, source_id)
                VALUES (?, ?, ?, ?, ?);
                """;

        try {

            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 deckSql
                         )) {

                statement.setString(
                        1,
                        deck.getId()
                                .toString()
                );

                statement.setString(
                        2,
                        deck.getCourseId()
                                .toString()
                );

                statement.setString(
                        3,
                        deck.getTopic()
                );

                statement.setInt(
                        4,
                        deck.getRequestedCount()
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 cardSql
                         )) {

                for (Flashcard card :
                        deck.getCards()) {

                    statement.setString(
                            1,
                            card.getId()
                                    .toString()
                    );

                    statement.setString(
                            2,
                            deck.getId()
                                    .toString()
                    );

                    statement.setString(
                            3,
                            card.getQuestion()
                    );

                    statement.setString(
                            4,
                            card.getAnswer()
                    );

                    statement.setString(
                            5,
                            card.getSourceId()
                                    .toString()
                    );

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();

            return deck;

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            throw new RuntimeException(
                    "Unable to save flashcard deck.",
                    e
            );

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    // =====================================================
    // F07 - QUIZZES
    // =====================================================

    public Quiz saveQuiz(
            Quiz quiz) {

        if (quiz == null) {

            throw new IllegalArgumentException(
                    "Quiz is required."
            );
        }

        if (!quiz.isComplete()) {

            throw new IllegalArgumentException(
                    "Cannot save an incomplete quiz."
            );
        }

        if (quiz.getQuestions()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot save an empty quiz."
            );
        }

        if (findCourse(
                quiz.getCourseId()) == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        String quizSql =
                """
                INSERT INTO quizzes
                (
                    id,
                    course_id,
                    topic,
                    difficulty,
                    requested_count
                )
                VALUES (?, ?, ?, ?, ?);
                """;

        String questionSql =
                """
                INSERT INTO quiz_questions
                (
                    id,
                    quiz_id,
                    question_order,
                    question_text,
                    option_a,
                    option_b,
                    option_c,
                    option_d,
                    correct_option_index,
                    explanation,
                    source_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try {

            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 quizSql
                         )) {

                statement.setString(
                        1,
                        quiz.getId()
                                .toString()
                );

                statement.setString(
                        2,
                        quiz.getCourseId()
                                .toString()
                );

                statement.setString(
                        3,
                        quiz.getTopic()
                );

                statement.setString(
                        4,
                        quiz.getDifficulty()
                );

                statement.setInt(
                        5,
                        quiz.getRequestedCount()
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 questionSql
                         )) {

                int order = 1;

                for (Question question :
                        quiz.getQuestions()) {

                    List<String> options =
                            question.getOptions();

                    statement.setString(
                            1,
                            question.getId()
                                    .toString()
                    );

                    statement.setString(
                            2,
                            quiz.getId()
                                    .toString()
                    );

                    statement.setInt(
                            3,
                            order
                    );

                    statement.setString(
                            4,
                            question.getText()
                    );

                    statement.setString(
                            5,
                            options.get(0)
                    );

                    statement.setString(
                            6,
                            options.get(1)
                    );

                    statement.setString(
                            7,
                            options.get(2)
                    );

                    statement.setString(
                            8,
                            options.get(3)
                    );

                    statement.setInt(
                            9,
                            question
                                    .getCorrectOptionIndex()
                    );

                    statement.setString(
                            10,
                            question.getExplanation()
                    );

                    statement.setString(
                            11,
                            question.getSourceId()
                                    .toString()
                    );

                    statement.addBatch();

                    order++;
                }

                statement.executeBatch();
            }

            connection.commit();

            return quiz;

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            throw new RuntimeException(
                    "Unable to save quiz.",
                    e
            );

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    // =====================================================
    // F08 - LOAD SAVED QUIZ
    // =====================================================

    public Quiz findQuiz(
            UUID quizId) {

        if (quizId == null) {
            return null;
        }

        String quizSql =
                """
                SELECT
                    id,
                    course_id,
                    topic,
                    difficulty,
                    requested_count
                FROM quizzes
                WHERE id = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             quizSql
                     )) {

            statement.setString(
                    1,
                    quizId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            if (!result.next()) {
                return null;
            }

            UUID id =
                    UUID.fromString(
                            result.getString("id")
                    );

            UUID courseId =
                    UUID.fromString(
                            result.getString(
                                    "course_id"
                            )
                    );

            String topic =
                    result.getString(
                            "topic"
                    );

            String difficulty =
                    result.getString(
                            "difficulty"
                    );

            int requestedCount =
                    result.getInt(
                            "requested_count"
                    );

            List<Question> questions =
                    loadQuizQuestions(
                            id
                    );

            return new Quiz(
                    id,
                    courseId,
                    topic,
                    difficulty,
                    requestedCount,
                    questions
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load quiz.",
                    e
            );
        }
    }

    private List<Question> loadQuizQuestions(
            UUID quizId)
            throws SQLException {

        List<Question> questions =
                new ArrayList<>();

        String sql =
                """
                SELECT
                    id,
                    question_text,
                    option_a,
                    option_b,
                    option_c,
                    option_d,
                    correct_option_index,
                    explanation,
                    source_id
                FROM quiz_questions
                WHERE quiz_id = ?
                ORDER BY question_order;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    quizId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                List<String> options =
                        List.of(
                                result.getString(
                                        "option_a"
                                ),
                                result.getString(
                                        "option_b"
                                ),
                                result.getString(
                                        "option_c"
                                ),
                                result.getString(
                                        "option_d"
                                )
                        );

                Question question =
                        new Question(
                                UUID.fromString(
                                        result.getString(
                                                "id"
                                        )
                                ),
                                result.getString(
                                        "question_text"
                                ),
                                options,
                                result.getInt(
                                        "correct_option_index"
                                ),
                                result.getString(
                                        "explanation"
                                ),
                                UUID.fromString(
                                        result.getString(
                                                "source_id"
                                        )
                                )
                        );

                questions.add(
                        question
                );
            }
        }

        return questions;
    }

    // =====================================================
    // F08 - QUIZ ATTEMPTS
    // =====================================================

    public QuizAttempt findAttempt(
            UUID attemptId) {

        if (attemptId == null) {
            return null;
        }

        String attemptSql =
                """
                SELECT
                    id,
                    quiz_id,
                    topic
                FROM quiz_attempts
                WHERE id = ?;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             attemptSql
                     )) {

            statement.setString(
                    1,
                    attemptId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            if (!result.next()) {
                return null;
            }

            UUID id =
                    UUID.fromString(
                            result.getString("id")
                    );

            UUID quizId =
                    UUID.fromString(
                            result.getString(
                                    "quiz_id"
                            )
                    );

            String topic =
                    result.getString(
                            "topic"
                    );

            List<QuestionResult> results =
                    loadAttemptResults(
                            id
                    );

            return new QuizAttempt(
                    id,
                    quizId,
                    topic,
                    results
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load quiz attempt.",
                    e
            );
        }
    }

    private List<QuestionResult>
            loadAttemptResults(
                    UUID attemptId)
            throws SQLException {

        List<QuestionResult> results =
                new ArrayList<>();

        String sql =
                """
                SELECT
                    question_id,
                    selected_option_index,
                    correct_option_index
                FROM quiz_attempt_results
                WHERE attempt_id = ?
                ORDER BY result_order;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    attemptId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                int selectedValue =
                        result.getInt(
                                "selected_option_index"
                        );

                Integer selectedOptionIndex =
                        result.wasNull()
                                ? null
                                : selectedValue;

                QuestionResult questionResult =
                        new QuestionResult(
                                UUID.fromString(
                                        result.getString(
                                                "question_id"
                                        )
                                ),
                                selectedOptionIndex,
                                result.getInt(
                                        "correct_option_index"
                                )
                        );

                results.add(
                        questionResult
                );
            }
        }

        return results;
    }

    public QuizAttempt recordAttempt(
            QuizAttempt attempt) {

        if (attempt == null) {

            throw new IllegalArgumentException(
                    "Quiz attempt is required."
            );
        }

        QuizAttempt existing =
                findAttempt(
                        attempt.getId()
                );

        if (existing != null) {

            return existing;
        }

        Quiz quiz =
                findQuiz(
                        attempt.getQuizId()
                );

        if (quiz == null) {

            throw new IllegalArgumentException(
                    "Unknown quiz ID."
            );
        }

        if (attempt.getResults().size()
                != quiz.getQuestions().size()) {

            throw new IllegalArgumentException(
                    "Attempt must contain one result "
                            + "for every quiz question."
            );
        }

        String attemptSql =
                """
                INSERT INTO quiz_attempts
                (
                    id,
                    quiz_id,
                    topic,
                    correct_count,
                    graded_count,
                    score_percent
                )
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        String resultSql =
                """
                INSERT INTO quiz_attempt_results
                (
                    attempt_id,
                    question_id,
                    result_order,
                    selected_option_index,
                    correct_option_index,
                    is_correct
                )
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        try {

            connection.setAutoCommit(
                    false
            );

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 attemptSql
                         )) {

                statement.setString(
                        1,
                        attempt.getId()
                                .toString()
                );

                statement.setString(
                        2,
                        attempt.getQuizId()
                                .toString()
                );

                statement.setString(
                        3,
                        attempt.getTopic()
                );

                statement.setInt(
                        4,
                        attempt.getCorrectCount()
                );

                statement.setInt(
                        5,
                        attempt.getGradedCount()
                );

                statement.setDouble(
                        6,
                        attempt.getScorePercent()
                );

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 resultSql
                         )) {

                int order = 1;

                for (QuestionResult result :
                        attempt.getResults()) {

                    statement.setString(
                            1,
                            attempt.getId()
                                    .toString()
                    );

                    statement.setString(
                            2,
                            result.getQuestionId()
                                    .toString()
                    );

                    statement.setInt(
                            3,
                            order
                    );

                    if (result
                            .getSelectedOptionIndex()
                            == null) {

                        statement.setNull(
                                4,
                                java.sql.Types.INTEGER
                        );

                    } else {

                        statement.setInt(
                                4,
                                result
                                        .getSelectedOptionIndex()
                        );
                    }

                    statement.setInt(
                            5,
                            result
                                    .getCorrectOptionIndex()
                    );

                    statement.setInt(
                            6,
                            result.isCorrect()
                                    ? 1
                                    : 0
                    );

                    statement.addBatch();

                    order++;
                }

                statement.executeBatch();
            }

            connection.commit();

            return attempt;

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            QuizAttempt repeated =
                    findAttempt(
                            attempt.getId()
                    );

            if (repeated != null) {

                return repeated;
            }

            throw new RuntimeException(
                    "Unable to save quiz attempt.",
                    e
            );

        } finally {

            try {
                connection.setAutoCommit(
                        true
                );
            } catch (SQLException ignored) {
            }
        }
    }

    // =====================================================
    // F09 - LOAD COMMITTED COURSE ATTEMPTS
    // =====================================================

    public List<QuizAttempt> loadAttempts(
            UUID courseId) {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (findCourse(courseId) == null) {

            throw new IllegalArgumentException(
                    "Unknown course ID."
            );
        }

        List<QuizAttempt> attempts =
                new ArrayList<>();

        String sql =
                """
                SELECT qa.id
                FROM quiz_attempts qa
                JOIN quizzes q
                    ON qa.quiz_id = q.id
                WHERE q.course_id = ?
                ORDER BY qa.rowid;
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    courseId.toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                UUID attemptId =
                        UUID.fromString(
                                result.getString(
                                        "id"
                                )
                        );

                QuizAttempt attempt =
                        findAttempt(
                                attemptId
                        );

                if (attempt == null) {

                    throw new IllegalStateException(
                            "Stored quiz attempt "
                                    + attemptId
                                    + " could not be reconstructed."
                    );
                }

                attempts.add(
                        attempt
                );
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load course quiz attempts.",
                    e
            );
        }

        return attempts;
    }
}