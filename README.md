# EECS 3311 Course Project — StudyBuddy Agent

AI study assistant that plans multi-step workflows over the student’s own notes (summaries, concepts, Q&A, flashcards, quizzes, progress).

**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent

## Stages

- **Stage 1 (Design)** — [`stage1/Stage1_Design_Report.md`](stage1/Stage1_Design_Report.md) and [`stage1/diagrams/`](stage1/diagrams/)
- **Stage 2 (Implementation)** — Java Maven app + [`stage2/stage2-report.md`](stage2/stage2-report.md) (export to PDF/Word as `stage2-report`)
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
mvn -q test
mvn -q exec:java -Dexec.args="cli"   # interactive CLI
mvn -q exec:java -Dexec.args="gui"   # Swing GUI
```

### CLI demo script (no API key required)

```text
course EECS 3311|EECS3311|FW26
upload <courseId> data/samples/os-scheduling.txt
materials <courseId>
demo-quiz <courseId>
quizzes <courseId>
take-quiz <quizId>
weak <courseId>
history <courseId>
plan <courseId>
nl What is Round Robin scheduling?
```

With a Groq key, also try: `summarize`, `concepts`, `flashcards`, `review`, AI `quiz`, and multi-step `nl summarize then make flashcards`.

### Free LLM setup (recommended: Groq)

1. Create a free key at https://console.groq.com/keys
2. `cp .env.example .env`
3. Set:
   ```bash
   LLM_PROVIDER=groq
   LLM_API_KEY=gsk_your_key_here
   LLM_MODEL=qwen/qwen3.8-27b
   ```
4. Run CLI/GUI — startup prints `StudyBuddy LLM: groq/...`
5. After AI calls, run `ailog` or inspect `data/ai-usage.log` for the Stage 2 cost log

Gemini: `LLM_PROVIDER=gemini` + Google AI Studio key.  
OpenRouter: `LLM_PROVIDER=openrouter`.  
Ollama: `LLM_PROVIDER=ollama` (local).  
Missing key → `stub/unconfigured` (deterministic features still work).

## Package layout

```
src/main/java/studybuddy/
  ui/         StudyGUI, StudyCLI, QuizView, ProgressView, FlashcardReview
  agent/      AgentController (Facade), Planner, ToolManager
  tools/      Tool, ToolContext, Result, *Tool
  llm/        LLMClient (Strategy), cloud/Ollama clients, PromptBuilder, AiUsageLog
  quiz/       QuizFactory (Factory Method), QuizEngine
  domain/     Course, Material, Concept, Flashcard, Question, ...
  store/      DocumentStore, JsonDocumentStore
  progress/   ProgressTracker (Observer subject)
```

## Stage 2 submission checklist

1. Source on this public GitHub repo  
2. README (this file)  
3. `.env.example` only — never commit `.env`  
4. `stage2/stage2-report.md` → export **`stage2-report.pdf`** (or Word)  
5. Demo videos for **every** feature (GUI + CLI collectively), links in the report  
6. Weekly code commits for Periods 1–3  

## Stage 1 diagrams

UMLet PNG + `.uxf` sources are in [`stage1/diagrams/`](stage1/diagrams/).
