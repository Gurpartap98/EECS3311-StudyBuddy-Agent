package studybuddy.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlannerTest {

    @Test
    void plansMultiStepSummarizeThenFlashcards() {
        Planner planner = new Planner();
        List<Planner.PlannedStep> steps =
                planner.plan("Please summarize my notes then make flashcards");
        assertEquals(2, steps.size());
        assertEquals("summarize", steps.get(0).toolName());
        assertEquals("flashcards", steps.get(1).toolName());
    }

    @Test
    void defaultsToRetrieveForQuestions() {
        Planner planner = new Planner();
        List<Planner.PlannedStep> steps = planner.plan("What is Round Robin scheduling?");
        assertEquals(1, steps.size());
        assertEquals("retrieve", steps.get(0).toolName());
        assertTrue(planner.parse("What is Round Robin?").question().contains("Round Robin"));
    }
}
