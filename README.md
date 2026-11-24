# EECS 3311 Course Project — StudyBuddy Agent

AI study assistant that plans multi-step workflows over the student’s own notes (summaries, concepts, Q&A, flashcards, quizzes, progress).

**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent

## Stages

- **Stage 1 (Design)** — [`stage1/Stage1_Design_Report.md`](stage1/Stage1_Design_Report.md) and [`stage1/diagrams/`](stage1/diagrams/)
- **Stage 2 (Implementation)** — Java Maven app + [`stage2-report.md`](stage2-report.md) / [`stage2-report.pdf`](stage2-report.pdf) (also under [`stage2/`](stage2/))
- Stage 3 (Testing) — forthcoming (requires this standalone CLI)

## Requirements

- JDK 17+ (JDK 21 works)
- Maven 3.8+
- Optional: a free/cheap LLM API key (Groq / Gemini / OpenRouter), or local Ollama

## Local configuration

```bash
cp .env.example .env
# Edit .env and set LLM_PROVIDER + LLM_API_KEY (never commit .env)
```

## Build the standalone Agent.jar

```bash
mvn -q -DskipTests package
# produces: target/Agent.jar  (fat JAR with all dependencies)
```

## Run — GUI

```bash
java -jar target/Agent.jar gui
# or
mvn -q exec:java -Dexec.args="gui"
```

## Run — interactive CLI

```bash
java -jar target/Agent.jar cli
# or
mvn -q exec:java -Dexec.args="cli"
```

### Interactive CLI demo script (matches recorded CLI video)

```text
course EECS3311|3311|FW26
courses
upload <courseId> data/samples/os-scheduling.txt
demo-quiz <courseId>
weak <courseId>
history <courseId>
plan <courseId>
quit
```

## Run — standalone CLI from a task file (Stage 2 update / Stage 3)

Professor-style example:

```bash
mvn -q -DskipTests package
java -jar target/Agent.jar -LLMAPI -input examples/task.txt
```

Equivalent forms:

```bash
java -jar target/Agent.jar cli --input examples/task.txt
java -jar target/Agent.jar cli -input examples/task.txt
```

### Supported options

| Option | Meaning |
|--------|---------|
| `gui` | Start Swing GUI |
| `cli` | Start CLI (interactive unless `-input` is set) |
| `-input <file>` / `--input <file>` | Run CLI commands from a task file (batch) |
| `-LLMAPI` / `--llm-api` | Use LLM from `.env` / environment (`LLM_PROVIDER` + `LLM_API_KEY`) |
| `help` / `-h` / `--help` | Show help |

Task files support comments (`# ...`) and placeholders filled during the run:

| Placeholder | Meaning |
|-------------|---------|
| `$COURSE` | Last created/selected course id |
| `$MATERIAL` | Last uploaded material id |
| `$QUIZ` | Last created quiz id |

See working example: [`examples/task.txt`](examples/task.txt).

You can also pipe commands on stdin:

```bash
java -jar target/Agent.jar cli < examples/task.txt
```

### Free LLM setup (recommended: Groq)

1. Create a free key at https://console.groq.com/keys  
2. `cp .env.example .env`  
3. Set:
   ```bash
   LLM_PROVIDER=groq
   LLM_API_KEY=gsk_your_key_here
   LLM_MODEL=qwen/qwen3.8-27b
   ```
4. Run GUI/CLI — startup prints `StudyBuddy LLM: groq/...`  
5. After AI calls, run `ailog` or inspect `data/ai-usage.log`

Missing key → `stub/unconfigured` (deterministic features still work: courses, upload, demo-quiz, grade, weak, history, plan).

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
examples/task.txt          Sample -input task file
stage2/screenshots/        Feature screenshots for the Stage 2 report
stage2/diagrams/           Updated UML (class, use-case, sequences)
```

## Demos

- **GUI then CLI (same task):** https://www.youtube.com/watch?v=umorkbnIZ_8  
- Per-feature GUI + CLI clips are indexed in [`stage2-report.md`](stage2-report.md) §7.

## Stage 2 artifacts

- Reports: [`stage2-report.md`](stage2-report.md) / [`stage2-report.pdf`](stage2-report.pdf)  
- Screenshots: [`stage2/screenshots/`](stage2/screenshots/)  
- Diagrams: [`stage2/diagrams/`](stage2/diagrams/)  
- Build: `mvn -q -DskipTests package` → `target/Agent.jar`
