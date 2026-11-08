package studybuddy.progress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject in the Observer pattern: records quiz outcomes and notifies views (F10/F11).
 * Optionally persists history to {@code progress.json} under a data directory.
 */
public final class ProgressTracker {

    private final List<ProgressObserver> observers = new CopyOnWriteArrayList<>();
    private final List<ProgressEvent> history = new ArrayList<>();
    private final Path persistenceFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProgressTracker() {
        this(null);
    }

    public ProgressTracker(Path dataDir) {
        this.persistenceFile = dataDir == null ? null : dataDir.resolve("progress.json");
        if (persistenceFile != null) {
            load();
        }
    }

    public void attach(ProgressObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void detach(ProgressObserver observer) {
        observers.remove(observer);
    }

    public void record(ProgressEvent event) {
        Objects.requireNonNull(event, "event");
        history.add(event);
        notifyObservers(event);
        save();
    }

    public void notifyObservers(ProgressEvent event) {
        for (ProgressObserver observer : observers) {
            observer.update(event);
        }
    }

    public List<ProgressEvent> history() {
        return List.copyOf(history);
    }

    public List<ProgressEvent> historyForCourse(String courseId) {
        return history.stream().filter(e -> e.getCourseId().equals(courseId)).toList();
    }

    /**
     * Topics whose average score is below 70% (F10).
     */
    public List<String> weakTopics(String courseId) {
        Map<String, List<Double>> byTopic = new HashMap<>();
        for (ProgressEvent event : history) {
            if (!event.getCourseId().equals(courseId)) {
                continue;
            }
            if (event.getTopic().isBlank()) {
                continue;
            }
            byTopic.computeIfAbsent(event.getTopic(), t -> new ArrayList<>())
                    .add(event.getScorePercent());
        }
        List<String> weak = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : byTopic.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(100);
            if (avg < 70.0) {
                weak.add(entry.getKey());
            }
        }
        weak.sort(Comparator.naturalOrder());
        return weak;
    }

    private void save() {
        if (persistenceFile == null) {
            return;
        }
        try {
            Files.createDirectories(persistenceFile.getParent());
            List<EventDto> dtos = history.stream().map(EventDto::from).toList();
            try (Writer writer = Files.newBufferedWriter(persistenceFile, StandardCharsets.UTF_8)) {
                gson.toJson(dtos, writer);
            }
        } catch (IOException ex) {
            System.err.println("Warning: could not save progress: " + ex.getMessage());
        }
    }

    private void load() {
        if (persistenceFile == null || !Files.isRegularFile(persistenceFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(persistenceFile, StandardCharsets.UTF_8)) {
            List<EventDto> dtos = gson.fromJson(reader, new TypeToken<List<EventDto>>() {
            }.getType());
            if (dtos == null) {
                return;
            }
            for (EventDto dto : dtos) {
                history.add(dto.toDomain());
            }
        } catch (IOException ex) {
            System.err.println("Warning: could not load progress: " + ex.getMessage());
        }
    }

    private record EventDto(String courseId, String topic, double scorePercent, String at) {
        static EventDto from(ProgressEvent e) {
            return new EventDto(e.getCourseId(), e.getTopic(), e.getScorePercent(), e.getAt().toString());
        }

        ProgressEvent toDomain() {
            ProgressEvent event = new ProgressEvent(courseId, topic, scorePercent);
            // ProgressEvent stamps Instant.now(); for loaded history we accept "now" skew
            // or keep DTO time via a package constructor — use reflection-free recreate:
            return new ProgressEvent(courseId, topic, scorePercent, Instant.parse(at));
        }
    }
}
