package studymate.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
import studymate.repository.StudyRepository;

public class QuizService {

    private final StudyRepository repository;

    public QuizService(
            StudyRepository repository) {

        if (repository == null) {

            throw new IllegalArgumentException(
                    "Study repository is required."
            );
        }

        this.repository = repository;
    }

    // =====================================================
    // F08 - DETERMINISTIC QUIZ GRADING
    // =====================================================

    public QuizAttempt grade(
            UUID quizId,
            UUID attemptId,
            List<Integer> selectedAnswers) {

        if (quizId == null) {

            throw new IllegalArgumentException(
                    "Quiz ID is required."
            );
        }

        if (attemptId == null) {

            throw new IllegalArgumentException(
                    "Attempt ID is required."
            );
        }

        // -------------------------------------------------
        // IDEMPOTENCY
        // -------------------------------------------------
        // If this exact attempt ID was already submitted,
        // return the existing result instead of grading
        // or saving it again.
        // -------------------------------------------------

        QuizAttempt existing =
                repository.findAttempt(
                        attemptId
                );

        if (existing != null) {

            if (!existing.getQuizId()
                    .equals(quizId)) {

                throw new IllegalArgumentException(
                        "Attempt ID already belongs "
                                + "to a different quiz."
                );
            }

            return existing;
        }

        Quiz quiz =
                repository.findQuiz(
                        quizId
                );

        if (quiz == null) {

            throw new IllegalArgumentException(
                    "Quiz was not found."
            );
        }

        if (!quiz.isComplete()) {

            throw new IllegalStateException(
                    "Saved quiz is incomplete "
                            + "and cannot be graded."
            );
        }

        if (selectedAnswers == null) {

            throw new IllegalArgumentException(
                    "Quiz answers are required."
            );
        }

        List<Question> questions =
                quiz.getQuestions();

        if (selectedAnswers.size()
                != questions.size()) {

            throw new IllegalArgumentException(
                    "You must provide one answer "
                            + "for every quiz question."
            );
        }

        List<QuestionResult> results =
                new ArrayList<>();

        for (int i = 0;
             i < questions.size();
             i++) {

            Question question =
                    questions.get(i);

            Integer selected =
                    selectedAnswers.get(i);

            // null represents a confirmed skipped answer.
            if (selected != null
                    && (selected < 0
                    || selected > 3)) {

                throw new IllegalArgumentException(
                        "Answer for question "
                                + (i + 1)
                                + " must be A, B, C, D, "
                                + "or skipped."
                );
            }

            QuestionResult result =
                    new QuestionResult(
                            question.getId(),
                            selected,
                            question
                                    .getCorrectOptionIndex()
                    );

            results.add(
                    result
            );
        }

        QuizAttempt attempt =
                new QuizAttempt(
                        attemptId,
                        quiz.getId(),
                        quiz.getTopic(),
                        results
                );

        return repository.recordAttempt(
                attempt
        );
    }
}