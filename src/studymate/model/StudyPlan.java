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
    private final int version;

    // =====================================================
    // NORMAL NEW PLAN
    // =====================================================

    public StudyPlan(
            UUID courseId,
            LocalDate examDate,
            String strategyName,
            List<StudySession> sessions,
            List<String> unscheduledTopics) {

        this(
                UUID.randomUUID(),
                courseId,
                examDate,
                strategyName,
                sessions,
                unscheduledTopics,
                1
        );
    }

    // =====================================================
    // REHYDRATE / VERSIONED PLAN
    // =====================================================

    public StudyPlan(
            UUID id,
            UUID courseId,
            LocalDate examDate,
            String strategyName,
            List<StudySession> sessions,
            List<String> unscheduledTopics,
            int version) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Plan ID is required."
            );
        }

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

        if (strategyName == null
                || strategyName.isBlank()) {

            throw new IllegalArgumentException(
                    "Strategy name is required."
            );
        }

        if (sessions == null) {

            throw new IllegalArgumentException(
                    "Study sessions are required."
            );
        }

        if (unscheduledTopics == null) {

            throw new IllegalArgumentException(
                    "Unscheduled topics are required."
            );
        }

        if (version < 1) {

            throw new IllegalArgumentException(
                    "Plan version must be at least 1."
            );
        }

        this.id =
                id;

        this.courseId =
                courseId;

        this.examDate =
                examDate;

        this.strategyName =
                strategyName;

        this.sessions =
                new ArrayList<>(
                        sessions
                );

        this.unscheduledTopics =
                new ArrayList<>(
                        unscheduledTopics
                );

        this.version =
                version;
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

    public int getVersion() {

        return version;
    }

    public boolean isFeasible() {

        return unscheduledTopics.isEmpty();
    }

    // =====================================================
    // CREATE NEXT VERSION OF SAME LOGICAL PLAN
    // =====================================================

    public StudyPlan nextVersion(
            LocalDate newExamDate,
            String newStrategyName,
            List<StudySession> newSessions,
            List<String> newUnscheduledTopics) {

        return new StudyPlan(
                id,
                courseId,
                newExamDate,
                newStrategyName,
                newSessions,
                newUnscheduledTopics,
                version + 1
        );
    }

    // =====================================================
    // RESTORE SNAPSHOT AS A NEW VERSION
    // =====================================================

    public StudyPlan restoreAsNextVersion(
            StudyPlan snapshot) {

        if (snapshot == null) {

            throw new IllegalArgumentException(
                    "Snapshot is required."
            );
        }

        if (!id.equals(
                snapshot.getId())) {

            throw new IllegalArgumentException(
                    "Snapshot belongs to a different plan."
            );
        }

        return new StudyPlan(
                id,
                courseId,
                snapshot.getExamDate(),
                snapshot.getStrategyName(),
                snapshot.getSessions(),
                snapshot.getUnscheduledTopics(),
                version + 1
        );
    }
}