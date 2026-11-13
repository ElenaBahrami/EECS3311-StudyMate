package studymate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlashcardDeck {

    private final UUID id;
    private final UUID courseId;
    private final String topic;
    private final int requestedCount;
    private final List<Flashcard> cards;

    public FlashcardDeck(
            UUID courseId,
            String topic,
            int requestedCount,
            List<Flashcard> cards) {

        if (courseId == null) {

            throw new IllegalArgumentException(
                    "Course ID is required."
            );
        }

        if (topic == null
                || topic.isBlank()) {

            throw new IllegalArgumentException(
                    "Topic is required."
            );
        }

        if (requestedCount < 1
                || requestedCount > 30) {

            throw new IllegalArgumentException(
                    "Flashcard count must be between 1 and 30."
            );
        }

        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.topic = topic;
        this.requestedCount =
                requestedCount;

        this.cards =
                new ArrayList<>(
                        cards
                );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTopic() {
        return topic;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public List<Flashcard> getCards() {
        return new ArrayList<>(
                cards
        );
    }

    public boolean isReducedDeck() {
        return cards.size()
                < requestedCount;
    }
}