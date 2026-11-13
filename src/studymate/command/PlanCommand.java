package studymate.command;

import studymate.model.StudyPlan;

public interface PlanCommand {

    StudyPlan execute();

    StudyPlan undo();

    boolean isExecuted();

    boolean isUndone();
}