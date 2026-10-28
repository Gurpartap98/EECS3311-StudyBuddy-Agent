package studybuddy.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import studybuddy.domain.Concept;
import studybuddy.domain.Course;
import studybuddy.domain.Flashcard;
import studybuddy.domain.Material;
import studybuddy.domain.Quiz;
import studybuddy.domain.StudyPlan;

/**
 * In-memory persistence for Stage 2 scaffolding (JSON file backend comes next).
 */
public class DocumentStore {

    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private final Map<String, List<Concept>> conceptsByMaterial = new LinkedHashMap<>();
    private final Map<String, List<Flashcard>> flashcardsByMaterial = new LinkedHashMap<>();
    private final Map<String, Quiz> quizzes = new LinkedHashMap<>();
    private final Map<String, StudyPlan> plans = new LinkedHashMap<>();

    public Course save(Course course) {
        Objects.requireNonNull(course, "course");
        courses.put(course.getId(), course);
        return course;
    }

    public Material save(Material material) {
        Objects.requireNonNull(material, "material");
        materials.put(material.getId(), material);
        Course course = courses.get(material.getCourseId());
        if (course != null) {
            course.addMaterialId(material.getId());
        }
        return material;
    }

    public Quiz save(Quiz quiz) {
        Objects.requireNonNull(quiz, "quiz");
        quizzes.put(quiz.getId(), quiz);
        return quiz;
    }

    public StudyPlan save(StudyPlan plan) {
        Objects.requireNonNull(plan, "plan");
        plans.put(plan.getId(), plan);
        return plan;
    }

    public void saveConcepts(String materialId, List<Concept> concepts) {
        Objects.requireNonNull(materialId, "materialId");
        conceptsByMaterial.put(materialId, new ArrayList<>(concepts));
    }

    public void saveFlashcards(String materialId, List<Flashcard> cards) {
        Objects.requireNonNull(materialId, "materialId");
        flashcardsByMaterial.put(materialId, new ArrayList<>(cards));
    }

    public Optional<Course> findCourse(String id) {
        return Optional.ofNullable(courses.get(id));
    }

    public Optional<Material> findMaterial(String id) {
        return Optional.ofNullable(materials.get(id));
    }

    public Optional<Quiz> findQuiz(String id) {
        return Optional.ofNullable(quizzes.get(id));
    }

    public List<Quiz> listQuizzes(String courseId) {
        return quizzes.values().stream()
                .filter(q -> q.getCourseId().equals(courseId))
                .toList();
    }

    public List<Course> listCourses() {
        return List.copyOf(courses.values());
    }

    public List<Material> listMaterials(String courseId) {
        return materials.values().stream()
                .filter(m -> m.getCourseId().equals(courseId))
                .toList();
    }

    public List<Concept> listConcepts(String materialId) {
        return List.copyOf(conceptsByMaterial.getOrDefault(materialId, List.of()));
    }

    public List<Flashcard> listFlashcards(String materialId) {
        return List.copyOf(flashcardsByMaterial.getOrDefault(materialId, List.of()));
    }
}
