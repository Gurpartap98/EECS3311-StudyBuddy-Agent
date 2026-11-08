package studybuddy.llm;

import java.util.Objects;

/**
 * Decorator that logs every {@link #generate(String)} call for the Stage 2 AI cost log.
 */
public final class LoggingLLMClient implements LLMClient {

    private final LLMClient delegate;
    private final String featureHint;
    private final double usdPerMillionTokens;

    public LoggingLLMClient(LLMClient delegate, double usdPerMillionTokens) {
        this(delegate, "generate", usdPerMillionTokens);
    }

    public LoggingLLMClient(LLMClient delegate, String featureHint, double usdPerMillionTokens) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.featureHint = featureHint == null ? "generate" : featureHint;
        this.usdPerMillionTokens = usdPerMillionTokens;
    }

    @Override
    public String generate(String prompt) throws LLMException {
        String completion = delegate.generate(prompt);
        AiUsageLog.get().record(delegate.describe(), featureHint, prompt, completion, usdPerMillionTokens);
        return completion;
    }

    @Override
    public String describe() {
        return delegate.describe();
    }
}
