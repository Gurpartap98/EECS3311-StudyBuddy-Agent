package studybuddy.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy implementation: OpenRouter OpenAI-compatible chat API (optional backup).
 */
public final class OpenRouterClient implements LLMClient {

    private static final String DEFAULT_MODEL = "meta-llama/llama-3.1-8b-instruct:free";
    private static final String URL = "https://openrouter.ai/api/v1/chat/completions";

    private final String apiKey;
    private final String model;

    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
    }

    @Override
    public String generate(String prompt) throws LLMException {
        if (prompt == null || prompt.isBlank()) {
            throw new LLMException("Prompt must not be blank");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        String response = HttpJson.postJson(
                URL,
                Map.of(
                        "Authorization", "Bearer " + apiKey,
                        "HTTP-Referer", "https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent",
                        "X-Title", "StudyBuddy Agent"),
                body);
        return extractContent(response);
    }

    private static String extractContent(String response) throws LLMException {
        try {
            JsonObject root = HttpJson.parseObject(response);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LLMException("OpenRouter response had no choices");
            }
            String content = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            if (content == null || content.isBlank()) {
                throw new LLMException("OpenRouter returned empty content");
            }
            return content.trim();
        } catch (LLMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LLMException("Failed to parse OpenRouter response: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String describe() {
        return "openrouter/" + model;
    }
}
