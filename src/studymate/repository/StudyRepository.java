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
import studymate.model.Material;

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
        }
    }

    // =====================================================
    // COURSE
    // =====================================================

    public Course saveCourse(Course course) {

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

    public Course findCourse(UUID courseId) {

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
    // MATERIAL
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
                                 materialSql)) {

                materialStatement.setString(
                        1,
                        material.getId().toString()
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
                                 chunkSql)) {

                for (Chunk chunk :
                        material.chunks()) {

                    chunkStatement.setString(
                            1,
                            chunk.getId().toString()
                    );

                    chunkStatement.setString(
                            2,
                            material.getId().toString()
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

    public List<Material> getMaterialsByCourse(
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

    public List<Material> getAllMaterials() {

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
                                "course_id")
                );

        String title =
                result.getString("title");

        String hash =
                result.getString("hash");

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
                    material.getId().toString()
            );

            ResultSet result =
                    statement.executeQuery();

            while (result.next()) {

                Chunk chunk =
                        new Chunk(
                                UUID.fromString(
                                        result.getString(
                                                "id")
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
}