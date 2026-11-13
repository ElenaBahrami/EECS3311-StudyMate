package studymate.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudyPlan {

    private final UUID id;
    private final UUID courseId;
    private final LocalDate examDate;
    private final String strategyName;

    private final List<StudySession> sessions;
    private final List<String> unscheduledTopics;

    public StudyPlan(
            UUID courseId,
            LocalDate examDate,
            String strategyName,
            List<StudySession> sessions,
            List<String> unscheduledTopics) {

        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.examDate = examDate;
        this.strategyName = strategyName;

        this.sessions =
                new ArrayList<>(
                        sessions
                );

        this.unscheduledTopics =
                new ArrayList<>(
                        unscheduledTopics
                );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public List<StudySession> getSessions() {
        return new ArrayList<>(
                sessions
        );
    }

    public List<String> getUnscheduledTopics() {
        return new ArrayList<>(
                unscheduledTopics
        );
    }

    public boolean isFeasible() {
        return unscheduledTopics.isEmpty();
    }
}