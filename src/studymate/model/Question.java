package studymate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Question {

    private final UUID id;
    private final String text;
    private final List<String> options;
    private final int correctOptionIndex;
    private final String explanation;
    private final UUID sourceId;

    public Question(
            String text,
            List<String> options,
            int correctOptionIndex,
            String explanation,
            UUID sourceId) {

        if (text == null
                || text.isBlank()) {

            throw new IllegalArgumentException(
                    "Question text is required."
            );
        }

        if (options == null
                || options.size() != 4) {

            throw new IllegalArgumentException(
                    "A quiz question must have exactly four options."
            );
        }

        for (String option : options) {

            if (option == null
                    || option.isBlank()) {

                throw new IllegalArgumentException(
                        "Quiz options cannot be empty."
                );
            }
        }

        if (correctOptionIndex < 0
                || correctOptionIndex > 3) {

            throw new IllegalArgumentException(
                    "Correct option index must be between 0 and 3."
            );
        }

        if (explanation == null
                || explanation.isBlank()) {

            throw new IllegalArgumentException(
                    "Question explanation is required."
            );
        }

        if (sourceId == null) {

            throw new IllegalArgumentException(
                    "Question source ID is required."
            );
        }

        this.id = UUID.randomUUID();
        this.text = text;

        this.options =
                new ArrayList<>(options);

        this.correctOptionIndex =
                correctOptionIndex;

        this.explanation =
                explanation;

        this.sourceId =
                sourceId;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {

        return new ArrayList<>(
                options
        );
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getCorrectOptionLetter() {

        return switch (correctOptionIndex) {

            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";

            default ->
                    throw new IllegalStateException(
                            "Invalid answer key."
                    );
        };
    }
}