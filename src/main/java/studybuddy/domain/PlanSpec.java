package studybuddy.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Input for study-plan generation (F06).
 */
public final class PlanSpec {

    private final String courseId;
    private final double hoursPerDay;
    private final LocalDate examDate;
    private final String focusTopic;

    public PlanSpec(String courseId, double hoursPerDay, LocalDate examDate, String focusTopic) {
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        if (hoursPerDay <= 0) {
            throw new IllegalArgumentException("hoursPerDay must be positive");
        }
        this.hoursPerDay = hoursPerDay;
        this.examDate = examDate;
        this.focusTopic = focusTopic == null ? "" : focusTopic.trim();
    }

    public String getCourseId() {
        return courseId;
    }

    public double getHoursPerDay() {
        return hoursPerDay;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public String getFocusTopic() {
        return focusTopic;
    }
}
