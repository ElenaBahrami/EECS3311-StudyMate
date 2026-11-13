package studymate.progress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import studymate.model.ProgressSummary;
import studymate.model.QuizAttempt;
import studymate.model.TopicProgress;
import studymate.repository.StudyRepository;

public class ProgressTracker {

    private final StudyRepository repository;

    private final List<ProgressListener> listeners =
            new ArrayList<>();

    public ProgressTracker(
            StudyRepository repository) {

        if (repository == null) {

            throw new IllegalArgumentException(
                    "Study repository is required."
            );
        }

        this.repository = repository;
    }

    // =====================================================
    // OBSERVER REGISTRATION
    // =====================================================

    public void addListener(
            ProgressListener listener) {

        if (listener == null) {

            throw new IllegalArgumentException(
                    "Progress listener is required."
            );
        }

        if (!listeners.contains(listener)) {

            listeners.add(
                    listener
            );
        }
    }

    public void removeListener(
            ProgressListener listener) {

        listeners.remove(
                listener
        );
    }

    // =====================================================
    // F09 - ANALYSE PROGRESS
    // =====================================================

    public ProgressSummary analyse(
            UUID courseId) {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        List<QuizAttempt> attempts =
                repository.loadAttempts(
                        courseId
                );

        if (attempts.isEmpty()) {

            return new ProgressSummary(
                    courseId,
                    List.of()
            );
        }

        Map<String, int[]> totalsByTopic =
                new LinkedHashMap<>();

        for (QuizAttempt attempt :
                attempts) {

            if (attempt == null) {

                throw new IllegalStateException(
                        "Stored progress contains "
                                + "an invalid quiz attempt."
                );
            }

            String topic =
                    attempt.getTopic();

            if (topic == null
                    || topic.isBlank()) {

                throw new IllegalStateException(
                        "Stored quiz attempt has "
                                + "no topic."
                );
            }

            int correct =
                    attempt.getCorrectCount();

            int graded =
                    attempt.getGradedCount();

            if (correct < 0
                    || graded < 0
                    || correct > graded) {

                throw new IllegalStateException(
                        "Stored quiz attempt contains "
                                + "invalid scoring data."
                );
            }

            int[] totals =
                    totalsByTopic.computeIfAbsent(
                            topic,
                            key -> new int[] {
                                    0,
                                    0
                            }
                    );

            totals[0] +=
                    correct;

            totals[1] +=
                    graded;
        }

        List<TopicProgress> topicProgress =
                new ArrayList<>();

        for (Map.Entry<String, int[]> entry :
                totalsByTopic.entrySet()) {

            String topic =
                    entry.getKey();

            int correct =
                    entry.getValue()[0];

            int graded =
                    entry.getValue()[1];

            topicProgress.add(
                    new TopicProgress(
                            topic,
                            correct,
                            graded
                    )
            );
        }

        topicProgress.sort(
                Comparator.comparing(
                        TopicProgress::getTopic,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        return new ProgressSummary(
                courseId,
                topicProgress
        );
    }

    // =====================================================
    // OBSERVER NOTIFICATION AFTER COMMITTED PROGRESS
    // =====================================================

    public ProgressSummary record(
            UUID courseId) {

        ProgressSummary summary =
                analyse(
                        courseId
                );

        notifyListeners(
                summary
        );

        return summary;
    }

    private void notifyListeners(
            ProgressSummary summary) {

        List<ProgressListener> snapshot =
                new ArrayList<>(
                        listeners
                );

        for (ProgressListener listener :
                snapshot) {

            listener.update(
                    summary
            );
        }
    }
}   