package studybuddy.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Question/answer flashcard generated from material (F07).
 */
public final class Flashcard {

    private final String id;
    private final String materialId;
    private final String front;
    private final String back;

    public Flashcard(String materialId, String front, String back) {
        this(UUID.randomUUID().toString(), materialId, front, back);
    }

    public Flashcard(String id, String materialId, String front, String back) {
        this.id = Objects.requireNonNull(id, "id");
        this.materialId = Objects.requireNonNull(materialId, "materialId");
        if (front == null || front.isBlank()) {
            throw new IllegalArgumentException("front must not be blank");
        }
        if (back == null || back.isBlank()) {
            throw new IllegalArgumentException("back must not be blank");
        }
        this.front = front.trim();
        this.back = back.trim();
    }

    public String getId() {
        return id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    @Override
    public String toString() {
        return "Flashcard{front='%s', back='%s'}".formatted(front, back);
    }
}
