package studybuddy.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import studybuddy.progress.ProgressEvent;
import studybuddy.progress.ProgressObserver;

/**
 * Observer view for progress events (F11).
 */
public final class ProgressView implements ProgressObserver {

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final List<ProgressEvent> events = new ArrayList<>();

    public JScrollPane getComponent() {
        return new JScrollPane(list);
    }

    @Override
    public void update(ProgressEvent event) {
        events.add(event);
        String line = "%s | %s | %.0f%%".formatted(
                event.getAt(), event.getTopic(), event.getScorePercent());
        SwingUtilities.invokeLater(() -> model.add(0, line));
    }

    public void show() {
        list.ensureIndexIsVisible(0);
    }

    public List<ProgressEvent> getEvents() {
        return List.copyOf(events);
    }
}
