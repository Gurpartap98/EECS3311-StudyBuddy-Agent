package studybuddy.llm;

/**
 * Builds the concrete {@link LLMClient} Strategy from {@link AppConfig}.
 */
public final class LLMClientFactory {

    private LLMClientFactory() {
    }

    public static LLMClient fromConfig(AppConfig config) {
        String provider = config.provider();
        return switch (provider) {
            case "groq" -> config.apiKey()
                    .<LLMClient>map(key -> new GroqClient(key, config.modelOrDefault("llama-3.1-8b-instant")))
                    .orElseGet(StubLLMClient::new);
            case "gemini" -> config.apiKey()
                    .<LLMClient>map(key -> new GeminiClient(key, config.modelOrDefault("gemini-2.0-flash")))
                    .orElseGet(StubLLMClient::new);
            case "ollama" -> new OllamaClient(config.ollamaBaseUrl(), config.ollamaModel());
            case "stub" -> new StubLLMClient();
            default -> {
                System.err.println("Unknown LLM_PROVIDER='" + provider + "', using stub.");
                yield new StubLLMClient();
            }
        };
    }
}
