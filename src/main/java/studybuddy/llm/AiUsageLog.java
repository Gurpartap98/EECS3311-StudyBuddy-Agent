package studybuddy.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Records LLM calls for the Stage 2 AI collaboration / cost log.
 * Estimates tokens roughly as words * 1.3 when the provider does not return usage.
 */
public final class AiUsageLog {

    public record Entry(
            Instant at,
            String provider,
            String feature,
            int promptChars,
            int completionChars,
            int estimatedTokens,
            double estimatedUsd) {
    }

    private static final AiUsageLog INSTANCE = new AiUsageLog();

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private Path file;

    private AiUsageLog() {
    }

    public static AiUsageLog get() {
        return INSTANCE;
    }

    public void setLogFile(Path file) {
        this.file = file;
    }

    public void record(String provider, String feature, String prompt, String completion, double usdPerMToken) {
        int promptChars = prompt == null ? 0 : prompt.length();
        int completionChars = completion == null ? 0 : completion.length();
        int estTokens = estimateTokens(prompt) + estimateTokens(completion);
        double usd = estTokens * usdPerMToken / 1_000_000.0;
        Entry entry = new Entry(
                Instant.now(), provider, feature, promptChars, completionChars, estTokens, usd);
        entries.add(entry);
        append(entry);
    }

    public List<Entry> entries() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }

    public double totalEstimatedUsd() {
        return entries().stream().mapToDouble(Entry::estimatedUsd).sum();
    }

    public int totalEstimatedTokens() {
        return entries().stream().mapToInt(Entry::estimatedTokens).sum();
    }

    public String summary() {
        return "AI usage: %d calls, ~%d tokens, ~$%.4f USD (estimated)"
                .formatted(entries().size(), totalEstimatedTokens(), totalEstimatedUsd());
    }

    private void append(Entry entry) {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            String line = String.format(
                    Locale.US,
                    "%s | %s | %s | promptChars=%d | completionChars=%d | ~tokens=%d | ~usd=%.6f%n",
                    entry.at(),
                    entry.provider(),
                    entry.feature(),
                    entry.promptChars(),
                    entry.completionChars(),
                    entry.estimatedTokens(),
                    entry.estimatedUsd());
            Files.writeString(
                    file,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Warning: could not write AI usage log: " + ex.getMessage());
        }
    }

    static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int words = text.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words * 1.3));
    }
}
