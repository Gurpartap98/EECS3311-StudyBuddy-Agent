package studybuddy.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressTrackerTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsHistoryAndComputesWeakTopics() {
        ProgressTracker tracker = new ProgressTracker(tempDir);
        tracker.record(new ProgressEvent("c1", "CPU Scheduling", 40));
        tracker.record(new ProgressEvent("c1", "CPU Scheduling", 50));
        tracker.record(new ProgressEvent("c1", "Memory", 90));

        assertTrue(tracker.weakTopics("c1").contains("CPU Scheduling"));
        assertEquals(3, tracker.history().size());

        ProgressTracker reloaded = new ProgressTracker(tempDir);
        assertEquals(3, reloaded.history().size());
        assertTrue(reloaded.weakTopics("c1").contains("CPU Scheduling"));
    }
}
