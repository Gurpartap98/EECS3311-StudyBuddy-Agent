package studybuddy.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import studybuddy.llm.LLMClient;
import studybuddy.store.DocumentStore;

/**
 * Shared runtime context passed to every {@link Tool#execute(ToolContext)}.
 */
public final class ToolContext {

    private final DocumentStore store;
    private final LLMClient llm;
    private final Map<String, Object> params;

    public ToolContext(DocumentStore store, LLMClient llm, Map<String, Object> params) {
        this.store = Objects.requireNonNull(store, "store");
        this.llm = Objects.requireNonNull(llm, "llm");
        this.params = params == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    public DocumentStore getStore() {
        return store;
    }

    public LLMClient getLlm() {
        return llm;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String requireString(String key) {
        Object value = params.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return String.valueOf(value);
    }

    public String optionalString(String key, String defaultValue) {
        Object value = params.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    public int optionalInt(String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
