package studymate.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
import studymate.progress.ProgressTracker;
import studymate.repository.StudyRepository;

public class QuizService {

    private final StudyRepository repository;
    private final ProgressTracker progressTracker;

    public QuizService(
            StudyRepository repository,
            ProgressTracker progressTracker) {

        if (repository == null) {

            throw new IllegalArgumentException(
                    "Study repository is required."
            );
        }

        if (progressTracker == null) {

            throw new IllegalArgumentException(
                    "Progress tracker is required."
            );
        }

        this.repository =
                repository;

        this.progressTracker =
                progressTracker;
    }

    // =====================================================
    // F08 - DETERMINISTIC QUIZ GRADING
    // F09 - NOTIFY PROGRESS AFTER COMMITTED ATTEMPT
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
        // If this exact attempt was already submitted,
        // return the stored result immediately.
        //
        // Progress is NOT notified again, so replaying an
        // attempt does not create another observation.
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

            // null means a confirmed skipped answer.
            // It remains a graded question worth zero
            // unless its answer is actually selected.
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

        QuizAttempt savedAttempt =
                repository.recordAttempt(
                        attempt
                );

        // -------------------------------------------------
        // OBSERVER / PROGRESS UPDATE
        // -------------------------------------------------
        // Only a NEW committed attempt reaches this point.
        // ProgressTracker reloads committed attempts,
        // recalculates deterministic topic progress,
        // and notifies registered ProgressListeners.
        // -------------------------------------------------

        progressTracker.record(
                quiz.getCourseId()
        );

        return savedAttempt;
    }
}