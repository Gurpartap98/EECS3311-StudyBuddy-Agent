package studybuddy.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import studybuddy.domain.Concept;
import studybuddy.domain.Course;
import studybuddy.domain.Flashcard;
import studybuddy.domain.MCQQuestion;
import studybuddy.domain.Material;
import studybuddy.domain.Question;
import studybuddy.domain.Quiz;
import studybuddy.domain.ShortAnswerQuestion;
import studybuddy.domain.StudyPlan;

/**
 * File-backed {@link DocumentStore} using Gson JSON under a data directory.
 */
public final class JsonDocumentStore extends DocumentStore {

    private final Path root;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private boolean loading;

    public JsonDocumentStore(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
            loading = true;
            loadAll();
            loading = false;
        } catch (IOException ex) {
            loading = false;
            throw new IllegalStateException("Cannot initialize data dir: " + root, ex);
        }
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public Course save(Course course) {
        Course saved = super.save(course);
        persist();
        return saved;
    }

    @Override
    public Material save(Material material) {
        Material saved = super.save(material);
        persist();
        return saved;
    }

    @Override
    public Quiz save(Quiz quiz) {
        Quiz saved = super.save(quiz);
        persist();
        return saved;
    }

    @Override
    public StudyPlan save(StudyPlan plan) {
        StudyPlan saved = super.save(plan);
        persist();
        return saved;
    }

    @Override
    public void saveConcepts(String materialId, List<Concept> concepts) {
        super.saveConcepts(materialId, concepts);
        persist();
    }

    @Override
    public void saveFlashcards(String materialId, List<Flashcard> cards) {
        super.saveFlashcards(materialId, cards);
        persist();
    }

    private void persist() {
        if (loading) {
            return;
        }
        try {
            writeJson("courses.json", listCourses().stream().map(CourseDto::from).toList());
            List<MaterialDto> materials = new ArrayList<>();
            for (Course c : listCourses()) {
                for (Material m : listMaterials(c.getId())) {
                    materials.add(MaterialDto.from(m));
                }
            }
            writeJson("materials.json", materials);

            Map<String, List<ConceptDto>> concepts = new LinkedHashMap<>();
            Map<String, List<FlashcardDto>> flashcards = new LinkedHashMap<>();
            for (Course c : listCourses()) {
                for (Material m : listMaterials(c.getId())) {
                    List<Concept> conceptList = listConcepts(m.getId());
                    if (!conceptList.isEmpty()) {
                        concepts.put(m.getId(), conceptList.stream().map(ConceptDto::from).toList());
                    }
                    List<Flashcard> cardList = listFlashcards(m.getId());
                    if (!cardList.isEmpty()) {
                        flashcards.put(m.getId(), cardList.stream().map(FlashcardDto::from).toList());
                    }
                }
            }
            writeJson("concepts.json", concepts);
            writeJson("flashcards.json", flashcards);

            List<QuizDto> quizzes = new ArrayList<>();
            List<PlanDto> plans = new ArrayList<>();
            for (Course c : listCourses()) {
                for (Quiz q : listQuizzes(c.getId())) {
                    quizzes.add(QuizDto.from(q));
                }
                for (StudyPlan p : listPlans(c.getId())) {
                    plans.add(PlanDto.from(p));
                }
            }
            writeJson("quizzes.json", quizzes);
            writeJson("plans.json", plans);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist store: " + ex.getMessage(), ex);
        }
    }

    private void loadAll() throws IOException {
        for (CourseDto dto : readList("courses.json", CourseDto.class)) {
            super.save(dto.toDomain());
        }
        for (MaterialDto dto : readList("materials.json", MaterialDto.class)) {
            super.save(dto.toDomain());
        }
        for (Map.Entry<String, List<ConceptDto>> e : readMap("concepts.json", ConceptDto.class).entrySet()) {
            super.saveConcepts(e.getKey(), e.getValue().stream().map(ConceptDto::toDomain).toList());
        }
        for (Map.Entry<String, List<FlashcardDto>> e : readMap("flashcards.json", FlashcardDto.class).entrySet()) {
            super.saveFlashcards(e.getKey(), e.getValue().stream().map(FlashcardDto::toDomain).toList());
        }
        for (QuizDto dto : readList("quizzes.json", QuizDto.class)) {
            super.save(dto.toDomain());
        }
        for (PlanDto dto : readList("plans.json", PlanDto.class)) {
            super.save(dto.toDomain());
        }
    }

    private void writeJson(String name, Object value) throws IOException {
        Path file = root.resolve(name);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(value, writer);
        }
    }

    private <T> List<T> readList(String name, Class<T> type) throws IOException {
        Path file = root.resolve(name);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        Type listType = TypeToken.getParameterized(List.class, type).getType();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<T> list = gson.fromJson(reader, listType);
            return list == null ? List.of() : list;
        }
    }

    private <T> Map<String, List<T>> readMap(String name, Class<T> type) throws IOException {
        Path file = root.resolve(name);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        Type mapType = TypeToken.getParameterized(
                Map.class,
                String.class,
                TypeToken.getParameterized(List.class, type).getType()).getType();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, List<T>> map = gson.fromJson(reader, mapType);
            return map == null ? Map.of() : map;
        }
    }

    private record CourseDto(
            String id, String name, String code, String term, boolean archived,
            String createdAt, List<String> materialIds) {
        static CourseDto from(Course c) {
            return new CourseDto(
                    c.getId(), c.getName(), c.getCode(), c.getTerm(), c.isArchived(),
                    c.getCreatedAt().toString(), new ArrayList<>(c.getMaterialIds()));
        }

        Course toDomain() {
            return new Course(id, name, code, term, archived, Instant.parse(createdAt), materialIds);
        }
    }

    private record MaterialDto(
            String id, String courseId, String title, String sourcePath, String extractedText,
            List<String> tags, String summary, String createdAt) {
        static MaterialDto from(Material m) {
            return new MaterialDto(
                    m.getId(), m.getCourseId(), m.getTitle(), m.getSourcePath(), m.getExtractedText(),
                    new ArrayList<>(m.getTags()), m.getSummary(), m.getCreatedAt().toString());
        }

        Material toDomain() {
            return new Material(
                    id, courseId, title, sourcePath, extractedText, tags, summary, Instant.parse(createdAt));
        }
    }

    private record ConceptDto(String id, String materialId, String name, String gloss) {
        static ConceptDto from(Concept c) {
            return new ConceptDto(c.getId(), c.getMaterialId(), c.getName(), c.getGloss());
        }

        Concept toDomain() {
            return new Concept(id, materialId, name, gloss);
        }
    }

    private record FlashcardDto(String id, String materialId, String front, String back) {
        static FlashcardDto from(Flashcard f) {
            return new FlashcardDto(f.getId(), f.getMaterialId(), f.getFront(), f.getBack());
        }

        Flashcard toDomain() {
            return new Flashcard(id, materialId, front, back);
        }
    }

    private record QuestionDto(
            String id, String type, String prompt, String topic, String difficulty,
            List<String> choices, int correctIndex, String expectedAnswer) {
        static QuestionDto from(Question q) {
            if (q instanceof MCQQuestion mcq) {
                return new QuestionDto(
                        q.getId(), "MCQ", q.getPrompt(), q.getTopic(), q.getDifficulty(),
                        new ArrayList<>(mcq.getChoices()), mcq.getCorrectIndex(), null);
            }
            if (q instanceof ShortAnswerQuestion sa) {
                return new QuestionDto(
                        q.getId(), "SHORT_ANSWER", q.getPrompt(), q.getTopic(), q.getDifficulty(),
                        List.of(), -1, sa.getExpectedAnswer());
            }
            throw new IllegalArgumentException("Unknown question type: " + q.getClass());
        }

        Question toDomain() {
            if ("MCQ".equals(type)) {
                return new MCQQuestion(id, prompt, topic, difficulty, choices, correctIndex);
            }
            return new ShortAnswerQuestion(id, prompt, topic, difficulty, expectedAnswer);
        }
    }

    private record QuizDto(
            String id, String courseId, String title, String createdAt, List<QuestionDto> questions) {
        static QuizDto from(Quiz q) {
            return new QuizDto(
                    q.getId(),
                    q.getCourseId(),
                    q.getTitle(),
                    q.getCreatedAt().toString(),
                    q.getQuestions().stream().map(QuestionDto::from).toList());
        }

        Quiz toDomain() {
            return new Quiz(
                    id,
                    courseId,
                    title,
                    questions.stream().map(QuestionDto::toDomain).toList(),
                    Instant.parse(createdAt));
        }
    }

    private record SessionDto(String date, String focusTopic, String task, int minutes) {
        static SessionDto from(StudyPlan.Session s) {
            return new SessionDto(s.date().toString(), s.focusTopic(), s.task(), s.minutes());
        }

        StudyPlan.Session toDomain() {
            return new StudyPlan.Session(LocalDate.parse(date), focusTopic, task, minutes);
        }
    }

    private record PlanDto(
            String id, String courseId, String examDate, double hoursPerDay,
            String createdAt, List<SessionDto> sessions) {
        static PlanDto from(StudyPlan p) {
            return new PlanDto(
                    p.getId(),
                    p.getCourseId(),
                    p.getExamDate() == null ? null : p.getExamDate().toString(),
                    p.getHoursPerDay(),
                    p.getCreatedAt().toString(),
                    p.getSessions().stream().map(SessionDto::from).toList());
        }

        StudyPlan toDomain() {
            LocalDate exam = examDate == null || examDate.isBlank() ? null : LocalDate.parse(examDate);
            return new StudyPlan(
                    id,
                    courseId,
                    exam,
                    hoursPerDay,
                    sessions.stream().map(SessionDto::toDomain).toList(),
                    Instant.parse(createdAt));
        }
    }
}
