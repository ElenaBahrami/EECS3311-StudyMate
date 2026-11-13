package studymate.planning;

import java.time.LocalDate;
import java.util.List;

import studymate.model.AvailabilitySlot;
import studymate.model.StudySession;
import studymate.model.StudyTopic;

public interface SchedulingStrategy {

    List<StudySession> allocate(
            List<StudyTopic> topics,
            List<AvailabilitySlot> availability,
            LocalDate examDate);

    String getName();
}