package studybuddy.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Key concept extracted from a material (F04).
 */
public final class Concept {

    private final String id;
    private final String materialId;
    private final String name;
    private final String gloss;

    public Concept(String materialId, String name, String gloss) {
        this(UUID.randomUUID().toString(), materialId, name, gloss);
    }

    public Concept(String id, String materialId, String name, String gloss) {
        this.id = Objects.requireNonNull(id, "id");
        this.materialId = Objects.requireNonNull(materialId, "materialId");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();
        this.gloss = gloss == null ? "" : gloss.trim();
    }

    public String getId() {
        return id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getName() {
        return name;
    }

    public String getGloss() {
        return gloss;
    }

    @Override
    public String toString() {
        return "Concept{name='%s', gloss='%s'}".formatted(name, gloss);
    }
}
