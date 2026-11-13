package studymate.planning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import studymate.model.AvailabilitySlot;
import studymate.model.StudyPlan;
import studymate.model.StudySession;
import studymate.model.StudyTopic;

public class StudyPlanner {

    public StudyPlan createPlan(
            UUID courseId,
            LocalDate examDate,
            List<StudyTopic> topics,
            List<AvailabilitySlot> availability,
            SchedulingStrategy strategy) {

        validateInputs(
                courseId,
                examDate,
                topics,
                availability,
                strategy
        );

        List<StudySession> sessions =
                strategy.allocate(
                        topics,
                        availability,
                        examDate
                );

        List<String> unscheduledTopics =
                findUnscheduledTopics(
                        topics,
                        sessions
                );

        StudyPlan plan =
                new StudyPlan(
                        courseId,
                        examDate,
                        strategy.getName(),
                        sessions,
                        unscheduledTopics
                );

        if (!validate(
                plan,
                availability)) {

            throw new IllegalStateException(
                    "Generated study plan violates "
                            + "the scheduling constraints."
            );
        }

        return plan;
    }

    private void validateInputs(
            UUID courseId,
            LocalDate examDate,
            List<StudyTopic> topics,
            List<AvailabilitySlot> availability,
            SchedulingStrategy strategy) {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (examDate == null) {

            throw new IllegalArgumentException(
                    "Exam date is required."
            );
        }

        if (!examDate.isAfter(
                LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Exam date must be in the future."
            );
        }

        if (topics == null
                || topics.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one study topic is required."
            );
        }

        if (availability == null
                || availability.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one availability slot is required."
            );
        }

        if (strategy == null) {

            throw new IllegalArgumentException(
                    "Scheduling strategy is required."
            );
        }
    }

    private List<String> findUnscheduledTopics(
            List<StudyTopic> topics,
            List<StudySession> sessions) {

        Map<String, Integer> allocatedMinutes =
                new HashMap<>();

        for (StudySession session :
                sessions) {

            allocatedMinutes.merge(
                    session.getTopic(),
                    session.getMinutes(),
                    Integer::sum
            );
        }

        List<String> unscheduled =
                new ArrayList<>();

        for (StudyTopic topic :
                topics) {

            int allocated =
                    allocatedMinutes
                            .getOrDefault(
                                    topic.getName(),
                                    0
                            );

            int remaining =
                    topic.getEstimatedMinutes()
                            - allocated;

            if (remaining > 0) {

                unscheduled.add(
                        topic.getName()
                                + " ("
                                + remaining
                                + " min remaining)"
                );
            }
        }

        return unscheduled;
    }

    public boolean validate(
            StudyPlan plan,
            List<AvailabilitySlot> availability) {

        if (plan == null) {
            return false;
        }

        Map<LocalDate, Integer> availableByDate =
                new HashMap<>();

        for (AvailabilitySlot slot :
                availability) {

            availableByDate.merge(
                    slot.getDate(),
                    slot.getAvailableMinutes(),
                    Integer::sum
            );
        }

        Map<LocalDate, Integer> scheduledByDate =
                new HashMap<>();

        for (StudySession session :
                plan.getSessions()) {

            if (session.getMinutes() <= 0) {
                return false;
            }

            if (!session.getDate()
                    .isBefore(
                            plan.getExamDate()
                    )) {

                return false;
            }

            scheduledByDate.merge(
                    session.getDate(),
                    session.getMinutes(),
                    Integer::sum
            );
        }

        for (Map.Entry<LocalDate, Integer> entry :
                scheduledByDate.entrySet()) {

            int available =
                    availableByDate
                            .getOrDefault(
                                    entry.getKey(),
                                    0
                            );

            if (entry.getValue()
                    > available) {

                return false;
            }
        }

        return true;
    }
}