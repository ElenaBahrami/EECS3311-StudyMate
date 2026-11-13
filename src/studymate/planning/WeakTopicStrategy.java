package studymate.planning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import studymate.model.AvailabilitySlot;
import studymate.model.StudySession;
import studymate.model.StudyTopic;

public class WeakTopicStrategy
        implements SchedulingStrategy {

    @Override
    public List<StudySession> allocate(
            List<StudyTopic> topics,
            List<AvailabilitySlot> availability,
            LocalDate examDate) {

        List<StudyTopic> orderedTopics =
                new ArrayList<>(
                        topics
                );

        /*
         * Topics with measured accuracy come first.
         * Lower accuracy means weaker performance,
         * so those topics are scheduled earlier.
         *
         * Topics without accuracy are placed after
         * measured topics.
         *
         * Java's stable sort preserves the original
         * syllabus order when scores are equal.
         */
        orderedTopics.sort(
                new Comparator<StudyTopic>() {

                    @Override
                    public int compare(
                            StudyTopic first,
                            StudyTopic second) {

                        boolean firstMeasured =
                                first.hasAccuracy();

                        boolean secondMeasured =
                                second.hasAccuracy();

                        if (firstMeasured
                                && secondMeasured) {

                            return Double.compare(
                                    first.getAccuracy(),
                                    second.getAccuracy()
                            );
                        }

                        if (firstMeasured) {
                            return -1;
                        }

                        if (secondMeasured) {
                            return 1;
                        }

                        return 0;
                    }
                }
        );

        List<AvailabilitySlot> sortedSlots =
                new ArrayList<>(
                        availability
                );

        sortedSlots.sort(
                Comparator.comparing(
                        AvailabilitySlot::getDate
                )
        );

        List<StudySession> sessions =
                new ArrayList<>();

        int topicIndex = 0;

        int remainingTopicMinutes =
                orderedTopics.isEmpty()
                        ? 0
                        : orderedTopics
                                .get(0)
                                .getEstimatedMinutes();

        for (AvailabilitySlot slot :
                sortedSlots) {

            if (!slot.getDate()
                    .isBefore(examDate)) {

                continue;
            }

            int remainingSlotMinutes =
                    slot.getAvailableMinutes();

            while (remainingSlotMinutes > 0
                    && topicIndex
                    < orderedTopics.size()) {

                StudyTopic topic =
                        orderedTopics.get(
                                topicIndex
                        );

                int sessionMinutes =
                        Math.min(
                                remainingSlotMinutes,
                                remainingTopicMinutes
                        );

                sessions.add(
                        new StudySession(
                                slot.getDate(),
                                topic.getName(),
                                sessionMinutes
                        )
                );

                remainingSlotMinutes -=
                        sessionMinutes;

                remainingTopicMinutes -=
                        sessionMinutes;

                if (remainingTopicMinutes == 0) {

                    topicIndex++;

                    if (topicIndex
                            < orderedTopics.size()) {

                        remainingTopicMinutes =
                                orderedTopics
                                        .get(topicIndex)
                                        .getEstimatedMinutes();
                    }
                }
            }
        }

        return sessions;
    }

    @Override
    public String getName() {
        return "Weak Topic";
    }
}