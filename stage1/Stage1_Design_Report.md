# EECS 3311 — Stage 1 Design Report  
## Project: AI Study & Learning Agent (`StudyBuddy Agent`)

**Course:** EECS 3311 Software Design (Fall 2026) — Song Wang  
**Student:** [YOUR NAME] · [STUDENT NUMBER]  
**Repository:** [PASTE PUBLIC GITHUB URL HERE]  
**LLM plan:** Ollama (local), primary model `llama3.2:3b` (fallback `llama3.2:1b`); optional cloud API if local unavailable  

---

## 1. Project Overview

### 1.1 Problem and motivation
University students accumulate lecture notes, PDFs, and slides but often struggle to turn that material into an effective study process. They need help summarizing long documents, generating practice questions, identifying weak topics, and building a study plan that fits their schedule. Doing this manually is slow and inconsistent.

### 1.2 Target users
- Primary: university students (e.g. EECS / STEM) who study from uploaded course materials  
- Secondary (optional): tutors who want to generate quizzes from a shared material set  

### 1.3 Agent description
**StudyBuddy Agent** is an AI agent-based study system (not a thin chatbot). The user interacts through a **GUI** and a **CLI**. The agent can:
- interpret natural-language study goals;
- **plan** multi-step study workflows (e.g. summarize → extract concepts → generate quiz);
- use **tools** (file load, summarize, quiz generate, grade, progress update);
- keep **memory** of courses, materials, quiz results, and past sessions;
- call a local **LLM via Ollama** for generation/reasoning steps.

Deterministic parts (file storage, grading MCQs, progress stats) stay in normal Java code. The LLM is used where language understanding/generation is needed.

### 1.4 Why an AI agent is appropriate
Studying from messy notes is open-ended: users ask different goals (“make a quiz on week 3”, “what am I weak on?”). An agent that plans steps and selects tools fits better than a single hardcoded menu flow. LLM calls alone are not enough — the system needs tools, memory, and structured domain objects (Course, Material, Quiz, Progress).

### 1.5 AI / LLM model(s)
| Role | Choice | Notes |
|------|--------|--------|
| Primary | Ollama + `llama3.2:3b` | Runs on student PC (16GB RAM); no paid API required |
| Lightweight fallback | Ollama + `llama3.2:1b` | If 3B is too slow |
| Optional backup | DeepSeek / Gemini API | Only if Ollama unavailable |

**How the LLM interacts with the system:**  
`GUI/CLI → AgentController → Planner → ToolManager / OllamaClient → domain objects → UI`.  
Prompts are built by `PromptBuilder`; structured outputs are parsed by `ResponseParser` into Java objects (summaries, quiz items, study plans).

### 1.6 Overall architecture (high level)
```
[StudyGUI] [StudyCLI]
        \     /
     AgentController   (Facade entry for UI)
            |
        Planner        (plans multi-step tasks)
       /    |    \
 Memory   ToolManager   OllamaClient
 Manager      |              |
         (tools...)     local LLM
 DocumentStore / QuizEngine / ProgressTracker
```

Interfaces (GUI + CLI) share the same controller so both expose major features.

---

## 2. Feature Specifications (≥10 meaningful features)

> Login / Logout / Exit / About do **not** count and are not listed as features.

### F01 — Create and Manage Courses
1. **ID/Name:** F01 Create and Manage Courses  
2. **Description:** Create, rename, list, and archive courses that own materials and study data.  
3. **User interaction (GUI):** Courses panel → New Course / Edit / Archive.  
4. **Input:** course name, optional code/term.  
5. **Output:** updated course list; selected course context.  
6. **AI involvement:** Deterministic.  
7. **Workflow:** User submits form → `CourseService` validates → `DocumentStore` persists → UI refreshes.  
8. **Errors:** empty name; duplicate course → show validation message; no change saved.

### F02 — Upload Learning Materials
1. **F02 Upload Learning Materials**  
2. Import PDF/TXT/MD into a course and extract text for later agent use.  
3. **GUI:** Course → Upload Material → file picker.  
4. **Input:** file path, course ID, optional title/tags.  
5. **Output:** stored `Material` with extracted text + metadata.  
6. **Deterministic** (text extraction); hybrid if PDF parse fails and user pastes text.  
7. **Workflow:** select file → `MaterialImportTool` extracts text → save in `DocumentStore` → confirm in UI.  
8. **Errors:** unsupported type, empty file, read failure → error dialog; material not added.

### F03 — Document Summarization
1. **F03 Document Summarization**  
2. Agent produces a concise summary of a selected material.  
3. **GUI:** Material viewer → Summarize button (or chat: “summarize week 2 notes”).  
4. **Input:** material ID; optional length preference.  
5. **Output:** summary text stored and shown.  
6. **AI-based.**  
7. **Workflow:** Controller → Planner chooses SummarizeTool → load text → `OllamaClient.generate` → save `Summary` → display.  
8. **Errors:** Ollama down → message + retry; empty material → refuse summarize.

### F04 — Concept Extraction
1. **F04 Concept Extraction**  
2. Extract key concepts/terms from a material into a structured list.  
3. **GUI:** Material → Extract Concepts.  
4. **Input:** material ID.  
5. **Output:** list of concepts (name + short gloss), saved to course.  
6. **AI-based.**  
7. **Workflow:** load material → prompt for concepts → parse into `Concept` objects → store → show list.  
8. **Errors:** parse failure → ask LLM to retry once; still fail → show raw text + warning.

### F05 — Question Answering over Materials
1. **F05 Q&A over Materials**  
2. Answer user questions using uploaded materials (retrieve relevant chunks, then LLM).  
3. **GUI:** Chat / Ask panel with course scope.  
4. **Input:** natural-language question; course/material scope.  
5. **Output:** answer + cited material titles/snippets.  
6. **Hybrid** (retrieval deterministic, answer AI).  
7. **Workflow:** retrieve top chunks (`RetrievalTool`) → build prompt with context → LLM → show answer + sources.  
8. **Errors:** no materials → prompt upload; low relevance → say “not found in materials” instead of hallucinating.

### F06 — Personalized Study-Plan Generation
1. **F06 Study-Plan Generation**  
2. Build a multi-day/session study plan from materials, deadlines, and weak topics.  
3. **GUI:** Study Plan → Generate; form for hours/day and exam date.  
4. **Input:** course ID, available hours, exam/due date, optional focus topics.  
5. **Output:** `StudyPlan` with ordered sessions/tasks.  
6. **AI / Hybrid** (plan structure may be validated deterministically).  
7. **Workflow:** gather progress + materials → Planner multi-step → LLM proposes plan → validate → save → display calendar/list.  
8. **Errors:** missing exam date → use default horizon; invalid hours → validation error.

### F07 — Flashcard Generation
1. **F07 Flashcard Generation**  
2. Generate flashcards (Q/A) from a material or concept list.  
3. **GUI:** Flashcards → Generate from material.  
4. **Input:** material/concept set; number of cards.  
5. **Output:** deck of flashcards.  
6. **AI-based.**  
7. **Workflow:** load content → LLM → parse cards → `FlashcardDeck` save → review UI.  
8. **Errors:** too few concepts → warn; malformed cards dropped with count shown.

### F08 — Quiz Generation
1. **F08 Quiz Generation**  
2. Generate a quiz (MCQ / short answer) from materials or weak topics.  
3. **GUI:** Quizzes → Generate Quiz.  
4. **Input:** course/material, question count, difficulty, type.  
5. **Output:** `Quiz` object displayed for taking.  
6. **AI-based.**  
7. **Workflow:** Planner → QuizTool → LLM → `QuizFactory` builds typed questions → open quiz view.  
8. **Errors:** LLM invalid JSON → retry/repair; else abort with message.

### F09 — Auto Grading and Answer Explanation
1. **F09 Auto Grading + Explanation**  
2. Grade submitted quiz; explain wrong answers (LLM for open-ended; deterministic for MCQ).  
3. **GUI:** After Submit Quiz → Results + Explain.  
4. **Input:** quiz attempt answers.  
5. **Output:** score, per-question feedback, optional explanations.  
6. **Hybrid.**  
7. **Workflow:** `QuizEngine.grade` → for wrong items optional `ExplainTool` via LLM → update `ProgressTracker` → show results.  
8. **Errors:** unanswered required Q → confirm submit; LLM explain fail → show correct answer only.

### F10 — Weak-Topic Identification
1. **F10 Weak-Topic Identification**  
2. Analyze quiz/flashcard history to flag weak topics.  
3. **GUI:** Progress → Weak Topics.  
4. **Input:** course ID; history window.  
5. **Output:** ranked weak topics + suggested review actions.  
6. **Hybrid** (stats deterministic; narrative AI optional).  
7. **Workflow:** aggregate scores by topic → threshold rules → optional LLM summary → display.  
8. **Errors:** insufficient history → ask user to take more quizzes.

### F11 — Progress Tracking and Session History
1. **F11 Progress Tracking / Session History**  
2. Record study sessions, quiz scores, time spent; show history.  
3. **GUI:** Progress dashboard / History list.  
4. **Input:** automatic from other features; filters.  
5. **Output:** charts/lists of progress over time.  
6. **Deterministic.**  
7. **Workflow:** events appended to `ProgressTracker` / `SessionLog` → query → render.  
8. **Errors:** corrupt log entry skipped; user notified if data missing.

### F12 — Natural-Language Study Commands (GUI chat + CLI)
1. **F12 NL Study Commands**  
2. User issues goals like “make 10 flashcards from Unit 2 and a short quiz”; agent plans and runs tools.  
3. **GUI:** Agent chat box. **CLI:** `studybuddy ask "..."`.  
4. **Input:** natural-language utterance + active course context.  
5. **Output:** executed plan summary + artifacts (plan/quiz/cards).  
6. **AI / Hybrid** (planning + tool use).  
7. **Workflow:** parse intent (LLM) → Planner builds steps → ToolManager executes → aggregate results → respond.  
8. **Errors:** unclear intent → clarifying question; tool failure → partial results + error; never silent fail.

---

## 3. Design Patterns (at least 5)

| # | Pattern | Problem | Participants | Why |
|---|---------|---------|--------------|-----|
| 1 | **MVC** | Separate UI from logic | View: `StudyGUI`/`StudyCLI`; Controller: `AgentController`; Model: Course, Material, Quiz, StudyPlan… | Same model for GUI+CLI; UI can change without rewriting agent |
| 2 | **Facade** | UI must not wire Planner+Tools+LLM | `AgentController` / `AgentFacade` | One simple API: `handleRequest()`, `generateQuiz()`, etc. |
| 3 | **Strategy** | Swap LLM backends / quiz styles | `LLMClient` <<interface>>; `OllamaClient`, `CloudLLMClient`; `QuizGenerationStrategy` | Open for extension (OCP); test with fake LLM |
| 4 | **Factory Method** | Create question/tool types without UI knowing classes | `QuizFactory`, `ToolFactory` | Centralize construction; easy to add ShortAnswer vs MCQ |
| 5 | **Observer** | Progress/UI must update when grading finishes | `ProgressTracker` (subject), GUI panels (observers) | Loose coupling for live progress updates |
| 6 | **Command** (bonus) | CLI/GUI actions undoable/queueable | `GenerateQuizCommand`, `SummarizeCommand` | Uniform execution + history of user actions |

> Stage 1 will emphasize patterns **1–5**. Each will be marked on the class diagram.

---

## 4. Use Cases (summary — expand fully in Section 6)

| UC | Name | Actor | Related features |
|----|------|-------|------------------|
| UC01 | Manage Course | Student | F01 |
| UC02 | Upload Material | Student | F02 |
| UC03 | Summarize Material | Student, Ollama | F03 |
| UC04 | Extract Concepts | Student, Ollama | F04 |
| UC05 | Ask Question over Materials | Student, Ollama | F05 |
| UC06 | Generate Study Plan | Student, Ollama | F06 |
| UC07 | Generate Flashcards | Student, Ollama | F07 |
| UC08 | Generate Quiz | Student, Ollama | F08 |
| UC09 | Take and Grade Quiz | Student, Ollama | F09, F10, F11 |
| UC10 | View Progress / Weak Topics | Student | F10, F11 |
| UC11 | NL Agent Command (GUI/CLI) | Student, Ollama | F12 (+ others) |

**Actors:** Student (primary); Ollama LLM Service (external); File System (external).

*(Full use-case descriptions with preconditions / main flow / alternatives are in `use_cases.md` — fill next.)*

---

## 5. Major Classes (for class diagram)

**Presentation:** `StudyGUI`, `StudyCLI`, `ChatPanel`, `QuizView`, `ProgressView`  
**Control / Agent:** `AgentController` (Facade), `Planner`, `MemoryManager`, `ToolManager`  
**LLM:** `LLMClient` (interface), `OllamaClient`, `CloudLLMClient`, `PromptBuilder`, `ResponseParser`  
**Tools:** `Tool` (interface), `SummarizeTool`, `ConceptTool`, `RetrievalTool`, `QuizTool`, `ExplainTool`, `MaterialImportTool`  
**Domain:** `Course`, `Material`, `Summary`, `Concept`, `Flashcard`, `FlashcardDeck`, `Quiz`, `Question`, `QuizAttempt`, `StudyPlan`, `StudySession`  
**Services / data:** `DocumentStore`, `QuizEngine`, `QuizFactory`, `ProgressTracker`, `SessionLog`  
**Patterns helpers:** `ProgressObserver`, `GenerateQuizCommand`, …

---

## 6. Sequence Diagrams to draw (minimum set)

| SD | Covers | Features |
|----|--------|----------|
| SD01 | Upload material | F02 |
| SD02 | Summarize material | F03 |
| SD03 | Q&A over materials | F05 |
| SD04 | Generate quiz | F08 |
| SD05 | Grade quiz + update progress | F09, F10, F11 |
| SD06 | NL multi-step command | F12, F06/F07 as planned |

Put images in `diagrams/` and reference them here.

---

## 7. Feature-to-Design Traceability Table

| Feature | Description | Type | Use Case | Classes | Key Methods | Seq | Patterns |
|---------|-------------|------|----------|---------|-------------|-----|----------|
| F01 | Manage courses | Det | UC01 | StudyGUI, CourseService, DocumentStore, Course | createCourse(), listCourses() | — | MVC |
| F02 | Upload materials | Det | UC02 | StudyGUI, MaterialImportTool, DocumentStore, Material | upload(), extractText(), save() | SD01 | Facade, Factory |
| F03 | Summarize | AI | UC03 | AgentController, Planner, SummarizeTool, OllamaClient, Material | summarize(), generate() | SD02 | Facade, Strategy |
| F04 | Concepts | AI | UC04 | AgentController, ConceptTool, OllamaClient, Concept | extractConcepts(), parse() | SD02* | Strategy, Factory |
| F05 | Q&A | Hybrid | UC05 | AgentController, RetrievalTool, PromptBuilder, OllamaClient | ask(), retrieve(), generate() | SD03 | Facade, Strategy |
| F06 | Study plan | AI/Hybrid | UC06 | AgentController, Planner, ProgressTracker, OllamaClient, StudyPlan | createPlan(), generate() | SD06 | Facade, Command |
| F07 | Flashcards | AI | UC07 | AgentController, FlashcardTool, FlashcardDeck, OllamaClient | generateDeck() | SD06 | Factory, Strategy |
| F08 | Quiz gen | AI | UC08 | AgentController, QuizTool, QuizFactory, OllamaClient, Quiz | generateQuiz(), createQuestions() | SD04 | Factory, Strategy, Facade |
| F09 | Grade + explain | Hybrid | UC09 | QuizView, QuizEngine, ExplainTool, ProgressTracker | grade(), explain() | SD05 | Observer, Strategy |
| F10 | Weak topics | Hybrid | UC10 | ProgressTracker, AgentController, OllamaClient | identifyWeakTopics() | SD05 | Observer |
| F11 | Progress history | Det | UC10 | ProgressTracker, SessionLog, ProgressView | record(), history() | SD05 | Observer, MVC |
| F12 | NL commands | AI/Hybrid | UC11 | StudyGUI/CLI, AgentController, Planner, ToolManager, LLMClient | handleRequest(), plan(), executeTool() | SD06 | Facade, Command, Strategy |

\*F04 may share SD02-style interaction or a small dedicated SD.

---

## 8. Feature Realization Explanations (short)

### F01 — Manage Courses
UC01. `StudyGUI` collects name → `CourseService.createCourse()` validates → `DocumentStore` persists `Course`. No LLM. MVC keeps UI thin.

### F02 — Upload Materials
UC02 / SD01. GUI picks file → `MaterialImportTool.extractText()` → `DocumentStore.save(Material)`. Errors surface in GUI.

### F03 — Summarize
UC03 / SD02. `AgentController.summarize(materialId)` → `Planner` selects `SummarizeTool` → tool loads text → `OllamaClient.generate` via `LLMClient` Strategy → summary returned to GUI.

### F04 — Concepts
Same agent path with `ConceptTool`; `ResponseParser` builds `Concept` list; stored on course.

### F05 — Q&A
UC05 / SD03. Retrieve chunks (`RetrievalTool`) → `PromptBuilder` → `OllamaClient` → answer + citations. If no chunks, refuse rather than invent.

### F06 — Study Plan
UC06. Controller gathers progress + constraints → Planner may call tools + LLM → `StudyPlan` validated and saved.

### F07 — Flashcards
LLM generates pairs → factory/parser builds `FlashcardDeck`.

### F08 — Quiz Generation
UC08 / SD04. `QuizTool` + `QuizFactory` create `Question` subtypes; Strategy selects MCQ vs short-answer style.

### F09 — Grade + Explain
UC09 / SD05. `QuizEngine.grade` deterministic for MCQ; `ExplainTool` optional LLM; `ProgressTracker` notifies Observers (GUI).

### F10 / F11 — Weak topics & history
Stats from attempts; optional LLM narrative; Progress view observes updates.

### F12 — NL Commands
UC11 / SD06. CLI/GUI send text → Facade `handleRequest` → Planner multi-step → `ToolManager` → combined result. Demonstrates real agent behavior.

---

## 9. AI use & academic integrity (for your awareness)

- Using LLMs / AI coding tools for **this course project is required**.  
- You must still submit **your own** design and (later) understood code.  
- Do **not** copy another student’s report/code.  
- If you paste text from slides/web, **acknowledge** the source.  
- Stage 2 will include an **AI–human collaboration log** — that is how required AI use is documented, not hidden.

---

## 10. Submission checklist

- [ ] This report completed and personalized (name, repo URL)  
- [ ] Class diagram image in `diagrams/`  
- [ ] Use-case diagram image in `diagrams/`  
- [ ] Sequence diagrams SD01–SD06 images  
- [ ] Full use-case writeups (see template below)  
- [ ] Public GitHub repo with this folder committed  
- [ ] eClass submission = repo URL  

---

## Appendix — Use-case description template (copy per UC)

```
Use Case ID: UC0X
Name:
Actor(s):
Goal:
Preconditions:
Trigger:
Main Success Scenario:
  1.
  2.
Alternative/Exception Flows:
Postconditions:
Related Feature(s):
```
