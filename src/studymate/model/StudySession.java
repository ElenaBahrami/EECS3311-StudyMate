package studymate.model;

import java.time.LocalDate;

public class StudySession {

    private final LocalDate date;
    private final String topic;
    private final int minutes;

    public StudySession(
            LocalDate date,
            String topic,
            int minutes) {

        if (date == null) {
            throw new IllegalArgumentException(
                    "Session date is required."
            );
        }

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Session topic is required."
            );
        }

        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Session duration must be positive."
            );
        }

        this.date = date;
        this.topic = topic;
        this.minutes = minutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTopic() {
        return topic;
    }

    public int getMinutes() {
        return minutes;
    }
}
