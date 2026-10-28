package studybuddy.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy implementation: Google Gemini generateContent API.
 */
public final class GeminiClient implements LLMClient {

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    private final String apiKey;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
    }

    @Override
    public String generate(String prompt) throws LLMException {
        if (prompt == null || prompt.isBlank()) {
            throw new LLMException("Prompt must not be blank");
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model
                + ":generateContent?key="
                + apiKey;

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));

        String response = HttpJson.postJson(url, Map.of(), body);
        return extractText(response);
    }

    private static String extractText(String response) throws LLMException {
        try {
            JsonObject root = HttpJson.parseObject(response);
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new LLMException("Gemini response had no candidates");
            }
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).getAsJsonObject().has("text")) {
                    out.append(parts.get(i).getAsJsonObject().get("text").getAsString());
                }
            }
            String text = out.toString().trim();
            if (text.isBlank()) {
                throw new LLMException("Gemini returned empty text");
            }
            return text;
        } catch (LLMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LLMException("Failed to parse Gemini response: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String describe() {
        return "gemini/" + model;
    }
}
