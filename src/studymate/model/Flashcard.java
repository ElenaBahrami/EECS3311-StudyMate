package studymate.model;

import java.util.UUID;

public class Flashcard {

    private final UUID id;
    private final String question;
    private final String answer;
    private final UUID sourceId;

    public Flashcard(
            String question,
            String answer,
            UUID sourceId) {

        if (question == null
                || question.isBlank()) {

            throw new IllegalArgumentException(
                    "Flashcard question is required."
            );
        }

        if (answer == null
                || answer.isBlank()) {

            throw new IllegalArgumentException(
                    "Flashcard answer is required."
            );
        }

        if (sourceId == null) {

            throw new IllegalArgumentException(
                    "Flashcard source ID is required."
            );
        }

        this.id = UUID.randomUUID();
        this.question = question;
        this.answer = answer;
        this.sourceId = sourceId;
    }

    public UUID getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public UUID getSourceId() {
        return sourceId;
    }
}