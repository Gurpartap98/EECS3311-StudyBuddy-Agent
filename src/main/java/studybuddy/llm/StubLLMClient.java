package studybuddy.llm;

/**
 * Temporary Strategy implementation used until a cloud/Ollama client is wired.
 * Returns an honest not-configured message — not fake study content.
 */
public final class StubLLMClient implements LLMClient {

    @Override
    public String generate(String prompt) throws LLMException {
        throw new LLMException(
                "No LLM provider configured. Copy .env.example to .env and set "
                        + "LLM_PROVIDER + LLM_API_KEY (or Ollama). Prompt length="
                        + (prompt == null ? 0 : prompt.length()));
    }

    @Override
    public String describe() {
        return "stub/unconfigured";
    }
}
