package studymate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizAttempt {

    private final UUID id;
    private final UUID quizId;
    private final String topic;
    private final List<QuestionResult> results;
    private final int correctCount;
    private final int gradedCount;

    public QuizAttempt(
            UUID id,
            UUID quizId,
            String topic,
            List<QuestionResult> results) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Attempt ID is required."
            );
        }

        if (quizId == null) {
            throw new IllegalArgumentException(
                    "Quiz ID is required."
            );
        }

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz topic is required."
            );
        }

        if (results == null
                || results.isEmpty()) {

            throw new IllegalArgumentException(
                    "Quiz results are required."
            );
        }

        this.id = id;
        this.quizId = quizId;
        this.topic = topic;

        this.results =
                new ArrayList<>(
                        results
                );

        int correct = 0;

        for (QuestionResult result :
                results) {

            if (result.isCorrect()) {
                correct++;
            }
        }

        this.correctCount = correct;

        // Skipped answers are still graded questions
        // and therefore receive zero credit.
        this.gradedCount =
                results.size();
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuizId() {
        return quizId;
    }

    public String getTopic() {
        return topic;
    }

    public List<QuestionResult> getResults() {

        return new ArrayList<>(
                results
        );
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getGradedCount() {
        return gradedCount;
    }

    public double getScorePercent() {

        return 100.0
                * correctCount
                / gradedCount;
    }
}