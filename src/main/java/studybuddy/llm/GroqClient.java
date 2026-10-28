package studybuddy.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy implementation: Groq OpenAI-compatible chat completions API.
 */
public final class GroqClient implements LLMClient {

    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;

    public GroqClient(String apiKey, String model) {
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
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt)));

        String response = HttpJson.postJson(
                URL,
                Map.of("Authorization", "Bearer " + apiKey),
                body);
        return extractContent(response);
    }

    private static String extractContent(String response) throws LLMException {
        try {
            JsonObject root = HttpJson.parseObject(response);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LLMException("Groq response had no choices");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            String content = message.get("content").getAsString();
            if (content == null || content.isBlank()) {
                throw new LLMException("Groq returned empty content");
            }
            return content.trim();
        } catch (LLMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LLMException("Failed to parse Groq response: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String describe() {
        return "groq/" + model;
    }
}
