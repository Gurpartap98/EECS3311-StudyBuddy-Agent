package studybuddy.progress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject in the Observer pattern: records quiz outcomes and notifies views (F10/F11).
 */
public final class ProgressTracker {

    private final List<ProgressObserver> observers = new CopyOnWriteArrayList<>();
    private final List<ProgressEvent> history = new ArrayList<>();

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
    }

    public void notifyObservers(ProgressEvent event) {
        for (ProgressObserver observer : observers) {
            observer.update(event);
        }
    }

    public List<ProgressEvent> history() {
        return List.copyOf(history);
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
}
