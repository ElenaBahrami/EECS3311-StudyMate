package studymate.progress;

import studymate.model.ProgressSummary;

public interface ProgressListener {

    void update(
            ProgressSummary summary
    );
}
