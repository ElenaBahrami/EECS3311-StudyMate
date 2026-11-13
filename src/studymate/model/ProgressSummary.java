package studymate.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ProgressSummary {

    private final UUID courseId;
    private final List<TopicProgress> topics;

    public ProgressSummary(
            UUID courseId,
            List<TopicProgress> topics) {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (topics == null) {

            throw new IllegalArgumentException(
                    "Topic progress list is required."
            );
        }

        this.courseId =
                courseId;

        this.topics =
                new ArrayList<>(
                        topics
                );
    }

    public UUID getCourseId() {

        return courseId;
    }

    public List<TopicProgress> getTopics() {

        return new ArrayList<>(
                topics
        );
    }

    public boolean isEmpty() {

        return topics.isEmpty();
    }

    public List<TopicProgress> getWeakTopics() {

        return topics.stream()
                .filter(
                        TopicProgress::isWeak
                )
                .sorted(
                        Comparator
                                .comparingDouble(
                                        TopicProgress
                                                ::getAccuracyPercent
                                )
                                .thenComparing(
                                        TopicProgress::getTopic
                                )
                )
                .toList();
    }

    public List<TopicProgress>
            getInsufficientDataTopics() {

        return topics.stream()
                .filter(
                        topic ->
                                !topic.hasEnoughEvidence()
                )
                .sorted(
                        Comparator.comparing(
                                TopicProgress::getTopic
                        )
                )
                .toList();
    }
}