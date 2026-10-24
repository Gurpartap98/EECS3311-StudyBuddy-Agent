package studybuddy.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A course owns materials and related study artefacts (F01).
 */
public final class Course {

    private final String id;
    private String name;
    private String code;
    private String term;
    private boolean archived;
    private final Instant createdAt;
    private final List<String> materialIds;

    public Course(String name, String code, String term) {
        this(UUID.randomUUID().toString(), name, code, term, false, Instant.now(), new ArrayList<>());
    }

    public Course(
            String id,
            String name,
            String code,
            String term,
            boolean archived,
            Instant createdAt,
            List<String> materialIds) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.code = code == null ? "" : code.trim();
        this.term = term == null ? "" : term.trim();
        this.archived = archived;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.materialIds = materialIds == null ? new ArrayList<>() : new ArrayList<>(materialIds);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireNonBlank(name, "name");
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? "" : code.trim();
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term == null ? "" : term.trim();
    }

    public boolean isArchived() {
        return archived;
    }

    public void archive() {
        this.archived = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getMaterialIds() {
        return Collections.unmodifiableList(materialIds);
    }

    public void addMaterialId(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        if (!materialIds.contains(materialId)) {
            materialIds.add(materialId);
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "Course{id='%s', name='%s', code='%s', term='%s', archived=%s}"
                .formatted(id, name, code, term, archived);
    }
}
