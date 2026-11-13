package studymate.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import studymate.agent.LearningAgent;
import studymate.command.RevisePlanCommand;
import studymate.model.AvailabilitySlot;
import studymate.model.StudyPlan;
import studymate.planning.SchedulingStrategy;
import studymate.repository.StudyRepository;

public class AgentController {

    private final LearningAgent learningAgent;
    private final StudyRepository repository;

    private RevisePlanCommand pendingCommand;
    private RevisePlanCommand lastExecutedCommand;

    public AgentController(
            LearningAgent learningAgent,
            StudyRepository repository) {

        if (learningAgent == null) {
            throw new IllegalArgumentException(
                    "Learning agent is required."
            );
        }

        if (repository == null) {
            throw new IllegalArgumentException(
                    "Study repository is required."
            );
        }

        this.learningAgent =
                learningAgent;

        this.repository =
                repository;
    }

    // =====================================================
    // F10 - ADAPT / PREVIEW
    // =====================================================

    public StudyPlan previewRevision(
            UUID courseId,
            LocalDate newExamDate,
            List<AvailabilitySlot> availability,
            SchedulingStrategy strategy)
            throws Exception {

        if (courseId == null) {
            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        StudyPlan currentPlan =
                repository.findLatestPlanByCourse(
                        courseId
                );

        if (currentPlan == null) {
            throw new IllegalStateException(
                    "No saved study plan exists "
                            + "for this course."
            );
        }

        StudyPlan candidate =
                learningAgent
                        .previewStudyPlanRevision(
                                currentPlan,
                                newExamDate,
                                availability,
                                strategy
                        );

        // Previewing an infeasible plan changes nothing.
        if (!candidate.isFeasible()) {

            pendingCommand =
                    null;

            return candidate;
        }

        pendingCommand =
                new RevisePlanCommand(
                        repository,
                        currentPlan,
                        candidate
                );

        return candidate;
    }

    // =====================================================
    // F10 - CONFIRM PENDING REVISION
    // =====================================================

    public StudyPlan confirmRevision() {

        if (pendingCommand == null) {

            throw new IllegalStateException(
                    "There is no pending plan "
                            + "revision to confirm."
            );
        }

        StudyPlan saved =
                pendingCommand.execute();

        lastExecutedCommand =
                pendingCommand;

        pendingCommand =
                null;

        return saved;
    }

    // =====================================================
    // F10 - REJECT PREVIEW
    // =====================================================

    public void rejectRevision() {

        pendingCommand =
                null;
    }

    // =====================================================
    // F10 - ONE-LEVEL UNDO
    // =====================================================

    public StudyPlan undoLastRevision() {

        if (lastExecutedCommand == null) {

            throw new IllegalStateException(
                    "There is no confirmed revision "
                            + "available to undo."
            );
        }

        if (lastExecutedCommand.isUndone()) {

            throw new IllegalStateException(
                    "The last revision was already undone."
            );
        }

        StudyPlan restored =
                lastExecutedCommand.undo();

        return restored;
    }

    // =====================================================
    // CONTROLLER STATE
    // =====================================================

    public boolean hasPendingRevision() {

        return pendingCommand != null;
    }

    public boolean canUndo() {

        return lastExecutedCommand != null
                && lastExecutedCommand.isExecuted()
                && !lastExecutedCommand.isUndone();
    }

    public StudyPlan getPendingPreview() {

        if (pendingCommand == null) {
            return null;
        }

        return pendingCommand
                .getRevisedPlan();
    }
}