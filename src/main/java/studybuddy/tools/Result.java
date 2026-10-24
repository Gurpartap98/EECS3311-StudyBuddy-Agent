package studybuddy.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Uniform tool outcome returned by {@link Tool#execute(ToolContext)}.
 */
public final class Result {

    private final boolean success;
    private final String message;
    private final Map<String, Object> data;

    private Result(boolean success, String message, Map<String, Object> data) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public static Result ok(String message) {
        return new Result(true, message, Map.of());
    }

    public static Result ok(String message, Map<String, Object> data) {
        return new Result(true, message, data);
    }

    public static Result fail(String message) {
        return new Result(false, message, Map.of());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Objects.requireNonNull(key, "key");
        return (T) data.get(key);
    }

    @Override
    public String toString() {
        return "Result{success=%s, message='%s', dataKeys=%s}"
                .formatted(success, message, data.keySet());
    }
}
