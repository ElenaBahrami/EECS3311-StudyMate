package studymate.model;

import java.time.LocalDate;

public class AvailabilitySlot {

    private final LocalDate date;
    private final int availableMinutes;

    public AvailabilitySlot(
            LocalDate date,
            int availableMinutes) {

        if (date == null) {
            throw new IllegalArgumentException(
                    "Availability date is required."
            );
        }

        if (availableMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Available minutes must be positive."
            );
        }

        this.date = date;
        this.availableMinutes =
                availableMinutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getAvailableMinutes() {
        return availableMinutes;
    }
}