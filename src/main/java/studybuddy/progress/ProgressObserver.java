package studybuddy.progress;

/** Observer notified when quiz/progress events are recorded (F11). */
public interface ProgressObserver {

    void update(ProgressEvent event);
}
