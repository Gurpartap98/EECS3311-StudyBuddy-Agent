# EECS 3311 — Stage 2 Implementation Report
## StudyBuddy Agent

**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent  
**Course:** EECS 3311 Software Design (Fall 2026), Section A — Song Wang  

> Export this file to PDF/Word as `stage2-report` before submission.

---

## 1. Project overview and implementation status

StudyBuddy Agent is a Java (Maven) AI study assistant with **Swing GUI** and **CLI**. Users manage courses and notes, then ask an agent facade (`AgentController`) to summarize, extract concepts, answer questions, generate flashcards/quizzes/study plans, grade attempts, and track weak topics.

**LLM integration:** Strategy pattern via `LLMClient` with `GroqClient`, `GeminiClient`, `OpenRouterClient`, `OllamaClient`, and `StubLLMClient`. Keys load from `.env` (gitignored). Calls are wrapped by `LoggingLLMClient` → `AiUsageLog` for token/cost estimates.

**Working now:**
- F01–F02 deterministic course/material management (TXT/MD/PDF)
- F08 demo quiz + interactive take/grade without LLM
- F09–F11 grading, explanations (deterministic helper + optional LLM), progress persistence, weak topics
- F06 plan fallback without LLM; AI path when key configured
- F03–F05, F07, F08 AI, F12 multi-step NL when `LLM_API_KEY` is set

**Limitations:** Retrieval is keyword overlap (not embeddings). AI JSON parsing is best-effort. Demo videos and final cost figures must be filled after local runs.

**Stage 1 → Stage 2 LLM change:** Primary runtime strategy is a free/cheap HTTP API (Groq) instead of only local Ollama 3B, same `LLMClient` interface (documented Strategy extension).

---

## 2. Feature to implementation mapping

| Feature | Status | Classes / key methods | Patterns | Video |
|---|---|---|---|---|
| F01 Create/manage courses | Implemented | `AgentController.createCourse/renameCourse/archiveCourse`, `DocumentStore.save`, GUI/CLI | MVC, Facade | https://youtu.be/2VU_iWYYCq4 |
| F02 Upload materials | Implemented | `AgentController.upload` → `MaterialImportTool.execute` → `extractText` (PDFBox) | Facade, Tool | https://youtu.be/2E8TiInSHKE |
| F03 Summarize | Implemented (AI) | `SummarizeTool.execute` → `PromptBuilder.buildSummaryPrompt` → `LLMClient.generate` | Strategy | https://youtu.be/JPmW0yr0Ypk |
| F04 Concepts | Implemented (AI) | `ConceptTool.execute` → `buildConceptPrompt` → `saveConcepts` | Strategy | https://youtu.be/PCu6bb5pAF8 |
| F05 Q&A | Implemented (Hybrid) | `RetrievalTool.execute` / `retrieve` → `PromptBuilder.buildQA` → `generate` | Strategy | https://youtu.be/sarW3iaAOv4 |
| F06 Study plan | Implemented (Hybrid) | `StudyPlanTool.execute` → `weakTopics` + LLM or deterministic fallback | Observer, Strategy | https://youtu.be/JVwXBfdMlbc |
| F07 Flashcards | Implemented (AI) | `FlashcardTool.execute` + `FlashcardReview` / CLI `review` | Strategy | https://youtu.be/3P0SAX4RVes |
| F08 Quiz generation | Implemented (AI + demo) | `QuizTool.execute` + Factory Method creators; `createDemoQuiz` | Factory Method | Demo: https://youtu.be/ZHwMFiRK9qk · AI: https://youtu.be/NP5qfThzWBs |
| F09 Grade + explain | Implemented (Hybrid) | `QuizEngine.grade` → `ExplainTool.explain` / `execute` → `ProgressTracker.record` | Observer | Demo quiz: https://youtu.be/GcM7pgDDRU8 · AI quiz: https://youtu.be/HB8ELp-WW3o |
| F10 Weak topics | Implemented (Hybrid) | `ProgressTracker.weakTopics` | Observer | https://youtu.be/NNKIedmG39c |
| F11 Progress history | Implemented | `ProgressTracker.record/history`, GUI History, CLI `history` | Observer | https://youtu.be/cM47AANV8Fc |
| F12 NL commands | Implemented (Hybrid) | `handleRequest` → `Planner.parse` → multi-step `ToolManager` | Facade, Tool | https://youtu.be/nSnLi25dHUg |

---

## 3. Implemented design patterns

1. **MVC** — Views: `StudyGUI`, `StudyCLI`, `QuizView`, `ProgressView`, `FlashcardReview`. Controller: `AgentController`. Model: domain + `DocumentStore` / `ProgressTracker`.
2. **Facade** — `AgentController` is the only UI entry (`createCourse`, `upload`, `summarize`, `handleRequest`, …).
3. **Strategy** — `LLMClient` with Groq/Gemini/OpenRouter/Ollama/Stub (+ `LoggingLLMClient` decorator).
4. **Factory Method** — Abstract `QuizFactory.createQuestion`; concrete `MCQQuestionFactory`, `ShortAnswerQuestionFactory`; products `MCQQuestion`, `ShortAnswerQuestion`.
5. **Observer** — `ProgressTracker` subject; `ProgressView` observer (`update`).

Class diagram: Stage 1 `stage1/diagrams/Class.png` plus code packages under `src/main/java/studybuddy/`. Update UMLet exports if the instructor requires a Stage 2 redraw.

---

## 4. Changes and rationale

| Original (Stage 1) | Implemented | Reason | Impact |
|---|---|---|---|
| Ollama `llama3.2:3b` primary | Groq/Gemini/OpenRouter via same `LLMClient` | Reliable demos on 16GB PC without local GPU | Document Strategy extension; Ollama still available |
| QuizFactory as simple factory labeled Factory Method | Full creator/product hierarchy | Stage 1 feedback (−2 / −4) | QuizTool never `new`s products |
| Tools with `explain`/`retrieve` only | Every tool implements `execute(ctx)` | Stage 1 feedback | Polymorphism complete |
| In-memory only | `JsonDocumentStore` + `progress.json` | Survive restarts for demos | Data under `./data` |
| Missing F01/F04/F07 sequences | Runtime flows in code + report method chains | Stage 1 sequence deductions | Fill diagrams optionally |

---

## 5. AI tools and collaboration log

| Tool | Model | Used for | Tokens / money |
|---|---|---|---|
| Cursor Cloud Agent | Composer / Cursor default | Stage 2 Java implementation, tests, report draft | Fill from Cursor usage if available |
| Runtime LLM (Groq recommended) | e.g. `qwen/qwen3.8-27b` | Summarize, concepts, Q&A, flashcards, quizzes, plans | See `data/ai-usage.log` after demos (`ailog` CLI) |

**How to capture cost:** run features with a key configured, then `ailog` or open `data/ai-usage.log`. Paste totals here before PDF export.

Student reviewed and accepts responsibility for all AI-generated code.

---

## 6. Development and verification summary

### Weekly commits (Toronto)

**Period 1 (20–26 Oct)** — scaffold, domain, facade/tools, CLI, GUI  
https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent/commits/main  

**Period 2 (27 Oct–2 Nov)** — LLM clients, PDF import, QuizEngine, JSON store, PromptBuilder/F12  
Same commits page; filter by date.

**Period 3 (3–9 Nov)** — interactive quiz, progress persistence, course manage/file chooser, flashcard review, AI usage log/OpenRouter  

Add direct commit links after push.

### Verification
- `mvn test` (QuizEngine, JsonDocumentStore, ProgressTracker, Planner, AiUsageLog)
- Manual CLI: course → upload sample → demo-quiz → take-quiz → weak/history
- Manual GUI: same paths + file chooser + Ask/NL

### Remaining
- Export this report to `stage2-report.pdf`
- Optional: update UMLet sequence diagrams for F01/F04/F06/F07

---

## 7. Demonstration index

Each feature was recorded as its own short YouTube clip (full clip = feature demo).  
Note: F09 AI grade, F10–F12 were uploaded on a second channel after the main channel hit the daily upload limit.

| Feature | Video URL | Timestamp | GUI / CLI shown |
|---|---|---|---|
| F01 Create/manage courses | https://youtu.be/2VU_iWYYCq4 | full clip | GUI |
| F02 Upload materials | https://youtu.be/2E8TiInSHKE | full clip | GUI |
| F03 Summarize (AI) | https://youtu.be/JPmW0yr0Ypk | full clip | GUI |
| F04 Concepts (AI) | https://youtu.be/PCu6bb5pAF8 | full clip | GUI |
| F05 Q&A (AI) | https://youtu.be/sarW3iaAOv4 | full clip | GUI |
| F06 Study plan | https://youtu.be/JVwXBfdMlbc | full clip | GUI |
| F07 Flashcards (AI) | https://youtu.be/3P0SAX4RVes | full clip | GUI |
| F08 Demo quiz | https://youtu.be/ZHwMFiRK9qk | full clip | GUI |
| F08 Quiz (AI) | https://youtu.be/NP5qfThzWBs | full clip | GUI |
| F09 Grade demo quiz | https://youtu.be/GcM7pgDDRU8 | full clip | GUI |
| F09 Grade Quiz (AI) | https://youtu.be/HB8ELp-WW3o | full clip | GUI |
| F10 Weak topics | https://youtu.be/NNKIedmG39c | full clip | GUI |
| F11 Progress history | https://youtu.be/cM47AANV8Fc | full clip | GUI |
| F12 NL multi-step (AI) | https://youtu.be/nSnLi25dHUg | full clip | GUI |

Do not show API keys on screen.
