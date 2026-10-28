package studybuddy.llm;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads local configuration from {@code .env} (gitignored) and process environment.
 */
public final class AppConfig {

    private final Dotenv dotenv;

    public AppConfig() {
        Path envFile = Path.of(".env");
        if (Files.isRegularFile(envFile)) {
            this.dotenv = Dotenv.configure().directory(".").ignoreIfMalformed().load();
        } else {
            this.dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
        }
    }

    public String provider() {
        return get("LLM_PROVIDER", "groq").trim().toLowerCase(Locale.ROOT);
    }

    public Optional<String> apiKey() {
        String key = get("LLM_API_KEY", "");
        if (key.isBlank() || key.contains("YOUR_LOCAL_API_KEY")) {
            return Optional.empty();
        }
        return Optional.of(key.trim());
    }

    public String modelOrDefault(String fallback) {
        String model = get("LLM_MODEL", "");
        return model.isBlank() ? fallback : model.trim();
    }

    public String ollamaBaseUrl() {
        return get("OLLAMA_BASE_URL", "http://localhost:11434").trim();
    }

    public String ollamaModel() {
        String model = get("OLLAMA_MODEL", "");
        return model.isBlank() ? "llama3.2:3b" : model.trim();
    }

    public Path dataDir() {
        return Path.of(get("STUDYBUDDY_DATA_DIR", "./data")).toAbsolutePath().normalize();
    }

    private String get(String key, String defaultValue) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDotenv = dotenv.get(key);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            return fromDotenv;
        }
        return defaultValue;
    }
}
