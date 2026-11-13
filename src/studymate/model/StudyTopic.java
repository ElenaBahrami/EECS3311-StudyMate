package studymate.model;

public class StudyTopic {

    private final String name;
    private final int estimatedMinutes;
    private final int priority;
    private final Double accuracy;

    public StudyTopic(
            String name,
            int estimatedMinutes,
            int priority) {

        this(
                name,
                estimatedMinutes,
                priority,
                null
        );
    }

    public StudyTopic(
            String name,
            int estimatedMinutes,
            int priority,
            Double accuracy) {

        if (name == null
                || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Topic name is required."
            );
        }

        if (estimatedMinutes <= 0) {

            throw new IllegalArgumentException(
                    "Estimated minutes must be positive."
            );
        }

        if (priority < 1
                || priority > 5) {

            throw new IllegalArgumentException(
                    "Priority must be between 1 and 5."
            );
        }

        if (accuracy != null
                && (accuracy < 0
                || accuracy > 100)) {

            throw new IllegalArgumentException(
                    "Accuracy must be between 0 and 100."
            );
        }

        this.name = name;
        this.estimatedMinutes =
                estimatedMinutes;
        this.priority = priority;
        this.accuracy = accuracy;
    }

    public String getName() {
        return name;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public int getPriority() {
        return priority;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public boolean hasAccuracy() {
        return accuracy != null;
    }
}