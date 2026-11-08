package studybuddy.llm;

/**
 * Builds the concrete {@link LLMClient} Strategy from {@link AppConfig}.
 */
public final class LLMClientFactory {

    private LLMClientFactory() {
    }

    public static LLMClient fromConfig(AppConfig config) {
        String provider = config.provider();
        LLMClient raw = switch (provider) {
            case "groq" -> config.apiKey()
                    .<LLMClient>map(key -> new GroqClient(key, config.modelOrDefault("llama-3.1-8b-instant")))
                    .orElseGet(StubLLMClient::new);
            case "gemini" -> config.apiKey()
                    .<LLMClient>map(key -> new GeminiClient(key, config.modelOrDefault("gemini-2.0-flash")))
                    .orElseGet(StubLLMClient::new);
            case "openrouter" -> config.apiKey()
                    .<LLMClient>map(key -> new OpenRouterClient(
                            key, config.modelOrDefault("meta-llama/llama-3.1-8b-instruct:free")))
                    .orElseGet(StubLLMClient::new);
            case "ollama" -> new OllamaClient(config.ollamaBaseUrl(), config.ollamaModel());
            case "stub" -> new StubLLMClient();
            default -> {
                System.err.println("Unknown LLM_PROVIDER='" + provider + "', using stub.");
                yield new StubLLMClient();
            }
        };
        // Approximate free-tier pricing placeholders for Stage 2 cost log
        double usdPerM = switch (provider) {
            case "groq" -> 0.05;
            case "gemini" -> 0.10;
            case "openrouter" -> 0.0;
            case "ollama" -> 0.0;
            default -> 0.0;
        };
        if (raw instanceof StubLLMClient) {
            return raw;
        }
        return new LoggingLLMClient(raw, usdPerM);
    }
}
