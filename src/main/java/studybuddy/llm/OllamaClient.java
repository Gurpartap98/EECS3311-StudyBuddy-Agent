package studybuddy.llm;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy implementation: local Ollama {@code /api/generate}.
 */
public final class OllamaClient implements LLMClient {

    private final String baseUrl;
    private final String model;

    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").replaceAll("/$", "");
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public String generate(String prompt) throws LLMException {
        if (prompt == null || prompt.isBlank()) {
            throw new LLMException("Prompt must not be blank");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("stream", false);

        String response = HttpJson.postJson(baseUrl + "/api/generate", Map.of(), body);
        try {
            JsonObject root = HttpJson.parseObject(response);
            if (!root.has("response")) {
                throw new LLMException("Ollama response missing 'response' field");
            }
            String text = root.get("response").getAsString();
            if (text == null || text.isBlank()) {
                throw new LLMException("Ollama returned empty response");
            }
            return text.trim();
        } catch (LLMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LLMException("Failed to parse Ollama response: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String describe() {
        return "ollama/" + model + "@" + baseUrl;
    }
}
