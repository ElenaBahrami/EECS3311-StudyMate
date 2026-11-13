package studymate.model;

import java.util.UUID;

public class QuestionResult {

    private final UUID questionId;
    private final Integer selectedOptionIndex;
    private final int correctOptionIndex;
    private final boolean correct;

    public QuestionResult(
            UUID questionId,
            Integer selectedOptionIndex,
            int correctOptionIndex) {

        if (questionId == null) {
            throw new IllegalArgumentException(
                    "Question ID is required."
            );
        }

        if (selectedOptionIndex != null
                && (selectedOptionIndex < 0
                || selectedOptionIndex > 3)) {

            throw new IllegalArgumentException(
                    "Selected option must be between 0 and 3."
            );
        }

        if (correctOptionIndex < 0
                || correctOptionIndex > 3) {

            throw new IllegalArgumentException(
                    "Correct option must be between 0 and 3."
            );
        }

        this.questionId = questionId;
        this.selectedOptionIndex = selectedOptionIndex;
        this.correctOptionIndex = correctOptionIndex;

        this.correct =
                selectedOptionIndex != null
                && selectedOptionIndex
                == correctOptionIndex;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public Integer getSelectedOptionIndex() {
        return selectedOptionIndex;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public boolean isCorrect() {
        return correct;
    }

    public boolean isSkipped() {
        return selectedOptionIndex == null;
    }
}