package studymate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quiz {

    private final UUID id;
    private final UUID courseId;
    private final String topic;
    private final String difficulty;
    private final int requestedCount;
    private final List<Question> questions;

    public Quiz(
            UUID courseId,
            String topic,
            String difficulty,
            int requestedCount,
            List<Question> questions) {

        this(
                UUID.randomUUID(),
                courseId,
                topic,
                difficulty,
                requestedCount,
                questions
        );
    }

    public Quiz(
            UUID id,
            UUID courseId,
            String topic,
            String difficulty,
            int requestedCount,
            List<Question> questions) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Quiz ID is required."
            );
        }

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz topic is required."
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

        if (questions == null) {

            throw new IllegalArgumentException(
                    "Quiz question list is required."
            );
        }

        if (questions.size()
                > requestedCount) {

            throw new IllegalArgumentException(
                    "Quiz contains more questions than requested."
            );
        }

        this.id = id;
        this.courseId = courseId;
        this.topic = topic;
        this.difficulty = normalizedDifficulty;
        this.requestedCount = requestedCount;

        this.questions =
                new ArrayList<>(
                        questions
                );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public List<Question> getQuestions() {

        return new ArrayList<>(
                questions
        );
    }

    public boolean isComplete() {

        return questions.size()
                == requestedCount;
    }
}