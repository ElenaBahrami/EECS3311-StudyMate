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
    // F06 - FLASHCARD DECKS
    // =====================================================

    public FlashcardDeck saveFlashcardDeck(
            FlashcardDeck deck) {

        if (deck == null) {

            throw new IllegalArgumentException(
                    "Flashcard deck is required."
            );
        }

        if (deck.getCards().isEmpty()) {

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
}