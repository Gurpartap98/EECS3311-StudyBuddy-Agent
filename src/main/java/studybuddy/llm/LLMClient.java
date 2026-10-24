package studybuddy.llm;

/**
 * Strategy interface for LLM providers (Ollama, Groq, Gemini, OpenRouter, ...).
 */
public interface LLMClient {

    /** Generate a completion for the given prompt. */
    String generate(String prompt) throws LLMException;

    /** Human-readable provider/model label for logs and demos. */
    String describe();
}
