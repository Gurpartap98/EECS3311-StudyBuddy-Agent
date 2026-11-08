package studybuddy.llm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiUsageLogTest {

    @Test
    void estimatesTokensAndRecordsUsd() {
        AiUsageLog log = AiUsageLog.get();
        int before = log.entries().size();
        log.record("test/provider", "unit", "one two three four five", "six seven", 1.0);
        assertTrue(log.entries().size() > before);
        assertTrue(log.totalEstimatedTokens() > 0);
        assertTrue(AiUsageLog.estimateTokens("hello world") >= 2);
    }
}
