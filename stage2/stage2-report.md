# EECS 3311 — Stage 2 Implementation Report
## StudyBuddy Agent

**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent  
**Course:** EECS 3311 Software Design (Fall 2026), Section A — Song Wang  

> Export this file to PDF/Word as `stage2-report` before submission.  
> Fill in video links/timestamps after you record demos.

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
| F01 Create/manage courses | Implemented | `AgentController.createCourse/renameCourse/archiveCourse`, `DocumentStore.save`, GUI/CLI | MVC, Facade | TODO |
| F02 Upload materials | Implemented | `AgentController.upload` → `MaterialImportTool.execute` → `extractText` (PDFBox) | Facade, Tool | TODO |
| F03 Summarize | Implemented (AI) | `SummarizeTool.execute` → `PromptBuilder.buildSummaryPrompt` → `LLMClient.generate` | Strategy | TODO |
| F04 Concepts | Implemented (AI) | `ConceptTool.execute` → `buildConceptPrompt` → `saveConcepts` | Strategy | TODO |
| F05 Q&A | Implemented (Hybrid) | `RetrievalTool.execute` / `retrieve` → `PromptBuilder.buildQA` → `generate` | Strategy | TODO |
| F06 Study plan | Implemented (Hybrid) | `StudyPlanTool.execute` → `weakTopics` + LLM or deterministic fallback | Observer, Strategy | TODO |
| F07 Flashcards | Implemented (AI) | `FlashcardTool.execute` + `FlashcardReview` / CLI `review` | Strategy | TODO |
| F08 Quiz generation | Implemented (AI + demo) | `QuizTool.execute` + Factory Method creators; `createDemoQuiz` | Factory Method | TODO |
| F09 Grade + explain | Implemented (Hybrid) | `QuizEngine.grade` → `ExplainTool.explain` / `execute` → `ProgressTracker.record` | Observer | TODO |
| F10 Weak topics | Implemented (Hybrid) | `ProgressTracker.weakTopics` | Observer | TODO |
| F11 Progress history | Implemented | `ProgressTracker.record/history`, GUI History, CLI `history` | Observer | TODO |
| F12 NL commands | Implemented (Hybrid) | `handleRequest` → `Planner.parse` → multi-step `ToolManager` | Facade, Tool | TODO |

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
| Runtime LLM (Groq recommended) | e.g. `llama-3.1-8b-instant` | Summarize, concepts, Q&A, flashcards, quizzes, plans | See `data/ai-usage.log` after demos (`ailog` CLI) |

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
- Record feature videos; paste links in §2 and §7
- Export this report to `stage2-report.pdf`
- Optional: update UMLet sequence diagrams for F01/F04/F06/F07

---

## 7. Demonstration index

| Feature | Video URL | Timestamp | GUI / CLI shown |
|---|---|---|---|
| F01 | TODO | | |
| F02 | TODO | | |
| F03 | TODO | | |
| F04 | TODO | | |
| F05 | TODO | | |
| F06 | TODO | | |
| F07 | TODO | | |
| F08 | TODO | | |
| F09 | TODO | | |
| F10 | TODO | | |
| F11 | TODO | | |
| F12 | TODO | | |

Collectively show **both GUI and CLI**. Do not show API keys on screen.
