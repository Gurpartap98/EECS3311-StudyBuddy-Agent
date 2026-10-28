# EECS 3311 Course Project — StudyBuddy Agent

AI study assistant that plans multi-step workflows over the student’s own notes (summaries, concepts, Q&A, flashcards, quizzes, progress).

**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent

## Stages

- **Stage 1 (Design)** — [`stage1/Stage1_Design_Report.md`](stage1/Stage1_Design_Report.md) and [`stage1/diagrams/`](stage1/diagrams/)
- **Stage 2 (Implementation)** — Java Maven app under `src/main/java/studybuddy/` (in progress)
- Stage 3 (Testing) — forthcoming

## Requirements

- JDK 17+ (JDK 21 works)
- Maven 3.8+
- Optional: a free/cheap LLM API key (Groq / Gemini / OpenRouter), or local Ollama

## Local configuration

```bash
cp .env.example .env
# Edit .env and set LLM_PROVIDER + LLM_API_KEY (never commit .env)
```

## Run

```bash
mvn -q compile
mvn -q exec:java -Dexec.args="cli"   # interactive CLI
mvn -q exec:java -Dexec.args="gui"   # Swing GUI
```

CLI quick start:

```text
course EECS 3311|EECS3311|FW26
upload <courseId> data/samples/os-scheduling.txt
materials <courseId>
ask What is Round Robin scheduling?
```

AI features (summarize / concepts / quiz / flashcards) need a configured LLM in `.env`.
Deterministic features (courses, upload TXT/MD, weak-topic stats, plan fallback) work without a key.

### Free LLM setup (recommended: Groq)

1. Create a free key at https://console.groq.com/keys
2. `cp .env.example .env`
3. Set:
   ```bash
   LLM_PROVIDER=groq
   LLM_API_KEY=gsk_your_key_here
   LLM_MODEL=llama-3.1-8b-instant
   ```
4. Run CLI/GUI — startup prints `StudyBuddy LLM: groq/...`

Gemini: set `LLM_PROVIDER=gemini` and a Google AI Studio key.  
Ollama: set `LLM_PROVIDER=ollama` (no API key; local server required).  
Missing/invalid key falls back to `stub/unconfigured` so the app still starts.

## Package layout

```
src/main/java/studybuddy/
  ui/         StudyGUI, StudyCLI, QuizView, ProgressView
  agent/      AgentController (Facade), Planner, ToolManager
  tools/      Tool, ToolContext, Result, *Tool
  llm/        LLMClient (Strategy), OllamaClient / cloud clients, PromptBuilder
  quiz/       QuizFactory (Factory Method), QuizEngine
  domain/     Course, Material, Concept, Flashcard, Question, ...
  store/      DocumentStore
  progress/   ProgressTracker (Observer subject)
```

## Stage 1 diagrams

UMLet PNG + `.uxf` sources are in [`stage1/diagrams/`](stage1/diagrams/). Sequence coverage for F01/F04/F06/F07 is being completed from Stage 1 feedback during Stage 2.
