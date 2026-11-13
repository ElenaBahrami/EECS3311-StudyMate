package studymate.command;

import studymate.model.StudyPlan;
import studymate.repository.StudyRepository;

public class RevisePlanCommand
        implements PlanCommand {

    private final StudyRepository repository;

    private final StudyPlan beforePlan;
    private final StudyPlan revisedPlan;

    private boolean executed;
    private boolean undone;

    public RevisePlanCommand(
            StudyRepository repository,
            StudyPlan beforePlan,
            StudyPlan revisedPlan) {

        if (repository == null) {

            throw new IllegalArgumentException(
                    "Study repository is required."
            );
        }

        if (beforePlan == null) {

            throw new IllegalArgumentException(
                    "Current plan is required."
            );
        }

        if (revisedPlan == null) {

            throw new IllegalArgumentException(
                    "Revised plan is required."
            );
        }

        if (!beforePlan.getId()
                .equals(
                        revisedPlan.getId()
                )) {

            throw new IllegalArgumentException(
                    "Revision must belong to "
                            + "the same study plan."
            );
        }

        if (!beforePlan.getCourseId()
                .equals(
                        revisedPlan.getCourseId()
                )) {

            throw new IllegalArgumentException(
                    "Revision must belong to "
                            + "the same course."
            );
        }

        if (revisedPlan.getVersion()
                != beforePlan.getVersion() + 1) {

            throw new IllegalArgumentException(
                    "Revision must be exactly "
                            + "one version newer."
            );
        }

        if (!revisedPlan.isFeasible()) {

            throw new IllegalArgumentException(
                    "Cannot create a command "
                            + "for an infeasible plan."
            );
        }

        this.repository =
                repository;

        this.beforePlan =
                beforePlan;

        this.revisedPlan =
                revisedPlan;

        this.executed =
                false;

        this.undone =
                false;
    }

    // =====================================================
    // COMMAND - EXECUTE REVISION
    // =====================================================

    @Override
    public StudyPlan execute() {

        if (executed) {

            throw new IllegalStateException(
                    "This revision command "
                            + "was already executed."
            );
        }

        StudyPlan current =
                repository.findPlan(
                        beforePlan.getId()
                );

        if (current == null) {

            throw new IllegalStateException(
                    "The study plan no longer exists."
            );
        }

        if (current.getVersion()
                != beforePlan.getVersion()) {

            throw new IllegalStateException(
                    "Plan version conflict. "
                            + "The plan changed after "
                            + "the preview was created."
            );
        }

        StudyPlan saved =
                repository.replacePlan(
                        revisedPlan,
                        beforePlan.getVersion()
                );

        executed =
                true;

        return saved;
    }

    // =====================================================
    // COMMAND - ONE-LEVEL UNDO
    // =====================================================

    @Override
    public StudyPlan undo() {

        if (!executed) {

            throw new IllegalStateException(
                    "Cannot undo a revision "
                            + "that was not executed."
            );
        }

        if (undone) {

            throw new IllegalStateException(
                    "This revision was already undone."
            );
        }

        StudyPlan current =
                repository.findPlan(
                        revisedPlan.getId()
                );

        if (current == null) {

            throw new IllegalStateException(
                    "The study plan no longer exists."
            );
        }

        if (current.getVersion()
                != revisedPlan.getVersion()) {

            throw new IllegalStateException(
                    "Undo conflict. "
                            + "The plan changed after "
                            + "this revision was saved."
            );
        }

        StudyPlan restored =
                current.restoreAsNextVersion(
                        beforePlan
                );

        StudyPlan saved =
                repository.replacePlan(
                        restored,
                        current.getVersion()
                );

        undone =
                true;

        return saved;
    }

    // =====================================================
    // COMMAND STATE
    // =====================================================

    @Override
    public boolean isExecuted() {

        return executed;
    }

    @Override
    public boolean isUndone() {

        return undone;
    }

    public StudyPlan getBeforePlan() {

        return beforePlan;
    }

    public StudyPlan getRevisedPlan() {

        return revisedPlan;
    }
}