package studybuddy.llm;

/** Raised when an LLM provider call fails. */
public class LLMException extends Exception {

    public LLMException(String message) {
        super(message);
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}
