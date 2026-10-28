package studybuddy.llm;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Small shared HTTP + JSON helpers for LLM providers. */
final class HttpJson {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private HttpJson() {
    }

    static String postJson(String url, Map<String, String> headers, Object body) throws LLMException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8));
            if (headers != null) {
                headers.forEach(builder::header);
            }
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new LLMException(
                        "HTTP " + response.statusCode() + " from LLM provider: "
                                + truncate(response.body(), 400));
            }
            return response.body();
        } catch (LLMException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LLMException("LLM HTTP call failed: " + ex.getMessage(), ex);
        }
    }

    static JsonObject parseObject(String json) {
        JsonElement element = JsonParser.parseString(json);
        return element.getAsJsonObject();
    }

    static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
