package studymate.planning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import studymate.model.AvailabilitySlot;
import studymate.model.StudySession;
import studymate.model.StudyTopic;

public class DeadlineStrategy
        implements SchedulingStrategy {

    @Override
    public List<StudySession> allocate(
            List<StudyTopic> topics,
            List<AvailabilitySlot> availability,
            LocalDate examDate) {

        List<StudySession> sessions =
                new ArrayList<>();

        List<AvailabilitySlot> sortedSlots =
                new ArrayList<>(
                        availability
                );

        sortedSlots.sort(
                Comparator.comparing(
                        AvailabilitySlot::getDate
                )
        );

        int topicIndex = 0;

        int remainingTopicMinutes =
                topics.isEmpty()
                        ? 0
                        : topics.get(0)
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
                    < topics.size()) {

                StudyTopic topic =
                        topics.get(
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
                            < topics.size()) {

                        remainingTopicMinutes =
                                topics
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
        return "Deadline";
    }
}