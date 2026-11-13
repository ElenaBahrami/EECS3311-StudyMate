package studymate.model;

public class TopicProgress {

    private final String topic;
    private final int correctCount;
    private final int gradedCount;

    public TopicProgress(
            String topic,
            int correctCount,
            int gradedCount) {

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Topic is required."
            );
        }

        if (correctCount < 0) {

            throw new IllegalArgumentException(
                    "Correct count cannot be negative."
            );
        }

        if (gradedCount < 0) {

            throw new IllegalArgumentException(
                    "Graded count cannot be negative."
            );
        }

        if (correctCount > gradedCount) {

            throw new IllegalArgumentException(
                    "Correct count cannot exceed graded count."
            );
        }

        this.topic =
                topic;

        this.correctCount =
                correctCount;

        this.gradedCount =
                gradedCount;
    }

    public String getTopic() {

        return topic;
    }

    public int getCorrectCount() {

        return correctCount;
    }

    public int getGradedCount() {

        return gradedCount;
    }

    public double getAccuracyPercent() {

        if (gradedCount == 0) {
            return 0.0;
        }

        return 100.0
                * correctCount
                / gradedCount;
    }

    public boolean hasEnoughEvidence() {

        return gradedCount >= 3;
    }

    public boolean isWeak() {

        return hasEnoughEvidence()
                && getAccuracyPercent() < 60.0;
    }

    public String getStatus() {

        if (!hasEnoughEvidence()) {

            return "INSUFFICIENT DATA";
        }

        if (isWeak()) {

            return "WEAK";
        }

        return "OK";
    }
}