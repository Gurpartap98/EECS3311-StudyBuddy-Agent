# EECS 3311 — Stage 1 Design Report
## StudyBuddy Agent: AI Study and Learning System

**Course:** EECS 3311 Software Design (Fall 2026), Section A — Song Wang  
**Student:** Gurpartap Singh Cheema (220454716)  
**Repository:** https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent  

---

## 1. Project Overview

### 1.1 Problem and Motivation
University students accumulate lecture notes, PDFs, and slides but often lack an effective process for turning that material into study activities. Manual summarization, quiz creation, and progress tracking are time-consuming and inconsistent. StudyBuddy Agent addresses this gap by combining deterministic software components with an LLM-based agent that plans multi-step study workflows over the user’s own materials.

### 1.2 Target Users
Primary users are university students who study from uploaded course materials. Secondary users may include tutors who generate quizzes from a shared material set.

### 1.3 Agent Description
StudyBuddy Agent is an AI agent-based system with both a graphical user interface (GUI) and a command-line interface (CLI). The agent interprets natural-language study goals, plans multi-step workflows, invokes tools (material import, summarization, quiz generation, retrieval, explanation, progress update), and calls a local LLM through Ollama. Deterministic components handle storage, MCQ grading, and progress statistics; the LLM is used for language understanding and generation.

### 1.4 Why an AI Agent Is Appropriate
Study goals are open-ended (for example, generate a quiz on a weak topic or summarize a week of notes). An agent that selects tools and executes multi-step plans fits this problem better than a single hardcoded flow or a thin chatbot that only forwards prompts to an LLM. The design requires structured domain objects (`Course`, `Material`, `Quiz`, progress records) in addition to model calls.

### 1.5 AI / LLM Model(s)
| Role | Model | Notes |
|------|-------|-------|
| Primary | Ollama `llama3.2:3b` | Local inference; no paid API required for typical workloads |
| Fallback | Ollama `llama3.2:1b` | Used if the 3B model is too slow on the target machine |
| Optional backup | DeepSeek or Gemini API | Used only if local Ollama is unavailable; would implement the same `LLMClient` interface |

The LLM is accessed through an `LLMClient` abstraction implemented by `OllamaClient`. `PromptBuilder` constructs prompts for tasks such as Q&A and summarization. Runtime path: GUI/CLI → `AgentController` → `Planner` / `ToolManager` → tools / `OllamaClient` → domain objects → UI.

### 1.6 Overall Architecture
The system follows an MVC-style layering with a shared agent facade. `StudyGUI` and `StudyCLI` are presentation components that delegate to `AgentController`. The controller coordinates `Planner`, `ToolManager`, and `LLMClient`. Tools such as `MaterialImportTool`, `SummarizeTool`, `QuizTool`, `RetrievalTool`, and `ExplainTool` encapsulate agent capabilities. Persistence and scoring are handled by `DocumentStore`, `QuizEngine`, and `ProgressTracker`. Both interfaces expose the same major features through the shared controller.

All UML diagrams were produced in **UMLet**. PNG exports are linked below; the corresponding `.uxf` source files are included in `stage1/diagrams/`.

---

## 2. Feature Specifications

Login, logout, exit, and about screens are excluded from the feature count.

### F01 — Create and Manage Courses
1. **ID / Name:** F01 Create and Manage Courses  
2. **Description:** Create, rename, list, and archive courses that own materials and study data.  
3. **User interaction (GUI):** Courses panel → New Course / Edit / Archive.  
4. **Input:** Course name; optional code and term.  
5. **Output:** Updated course list and active course context.  
6. **AI involvement:** Deterministic.  
7. **Expected workflow:** User submits the form; `AgentController` / GUI validates; `DocumentStore` persists a `Course`; UI refreshes.  
8. **Error / alternative cases:** Empty or duplicate name produces a validation message; no change is saved.

### F02 — Upload Learning Materials
1. **ID / Name:** F02 Upload Learning Materials  
2. **Description:** Import PDF, TXT, or Markdown into a course and extract text for later agent use.  
3. **User interaction (GUI):** Course → Upload Material → file picker.  
4. **Input:** File path, course ID, optional title and tags.  
5. **Output:** Stored `Material` with extracted text and metadata.  
6. **AI involvement:** Deterministic for extraction; hybrid if the user pastes text after a parse failure.  
7. **Expected workflow:** Select file → `AgentController.upload` → `MaterialImportTool.extractText()` → `DocumentStore.save()` → confirmation.  
8. **Error / alternative cases:** Unsupported type, empty file, or read failure shows an error; material is not added.

### F03 — Document Summarization
1. **ID / Name:** F03 Document Summarization  
2. **Description:** Produce a concise summary of a selected material.  
3. **User interaction (GUI):** Material viewer → Summarize, or natural-language request in chat.  
4. **Input:** Material ID; optional length preference.  
5. **Output:** Summary text, stored and displayed.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** `AgentController` → `Planner` → `SummarizeTool` → `OllamaClient.generate` → save via `DocumentStore` → display.  
8. **Error / alternative cases:** Ollama unavailable → error with retry; empty material → refuse summarization.

### F04 — Concept Extraction
1. **ID / Name:** F04 Concept Extraction  
2. **Description:** Extract key concepts from a material into a structured list.  
3. **User interaction (GUI):** Material → Extract Concepts.  
4. **Input:** Material ID.  
5. **Output:** List of concepts (name and short gloss), saved with the course material set.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** `AgentController` loads material → `PromptBuilder` / LLM prompt via `OllamaClient` → parse concept list → store through `DocumentStore` → display.  
8. **Error / alternative cases:** Parse failure triggers one retry; persistent failure shows raw output with a warning.

### F05 — Question Answering over Materials
1. **ID / Name:** F05 Q&A over Materials  
2. **Description:** Answer questions using uploaded materials (retrieve relevant chunks, then generate an answer).  
3. **User interaction (GUI):** Ask / Chat panel scoped to a course.  
4. **Input:** Natural-language question; course or material scope.  
5. **Output:** Answer with cited material titles or snippets.  
6. **AI involvement:** Hybrid (deterministic retrieval; AI answer generation).  
7. **Expected workflow:** `RetrievalTool.retrieve` → `PromptBuilder.buildQA` → `OllamaClient.generate` → display answer and sources.  
8. **Error / alternative cases:** No materials → prompt upload; low relevance → report that the answer was not found in materials.

### F06 — Personalized Study-Plan Generation
1. **ID / Name:** F06 Study-Plan Generation  
2. **Description:** Build a multi-session study plan from materials, deadlines, and weak topics.  
3. **User interaction (GUI):** Study Plan → Generate; form for hours per day and exam date.  
4. **Input:** Course ID, available hours, exam or due date, optional focus topics.  
5. **Output:** Ordered study sessions and tasks (stored as plan data via `DocumentStore`).  
6. **AI involvement:** AI / hybrid (plan validated deterministically).  
7. **Expected workflow:** Gather progress (`ProgressTracker`) and materials → `Planner` / LLM proposal → validate → save → display.  
8. **Error / alternative cases:** Missing exam date uses a default horizon; invalid hours produce a validation error.

### F07 — Flashcard Generation
1. **ID / Name:** F07 Flashcard Generation  
2. **Description:** Generate question/answer flashcards from a material or concept list.  
3. **User interaction (GUI):** Flashcards → Generate from material.  
4. **Input:** Material or concept set; number of cards.  
5. **Output:** Flashcard deck data ready for review.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** Load content → `AgentController` / tool path → `OllamaClient.generate` → parse card pairs → save → open review UI.  
8. **Error / alternative cases:** Too few concepts → warning; malformed cards are dropped and counted.

### F08 — Quiz Generation
1. **ID / Name:** F08 Quiz Generation  
2. **Description:** Generate a quiz (MCQ or short answer) from materials or weak topics.  
3. **User interaction (GUI):** Quizzes → Generate Quiz.  
4. **Input:** Course or material, question count, difficulty, question type.  
5. **Output:** `Quiz` ready for the student to take.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** `QuizTool` → `OllamaClient.generate` → `QuizFactory.createQuestions` → `QuizView.open`.  
8. **Error / alternative cases:** Invalid model JSON → retry or repair; otherwise abort with an error message.

### F09 — Automatic Grading and Answer Explanation
1. **ID / Name:** F09 Auto Grading and Explanation  
2. **Description:** Grade a submitted quiz and explain incorrect answers.  
3. **User interaction (GUI):** After Submit → Results and Explain.  
4. **Input:** Quiz attempt answers.  
5. **Output:** Score, per-question feedback, optional explanations.  
6. **AI involvement:** Hybrid (deterministic MCQ grading; AI explanations for open-ended or missed items).  
7. **Expected workflow:** `QuizEngine.grade` → optional `ExplainTool.explain` → `ProgressTracker.record` → notify `ProgressView` → display results.  
8. **Error / alternative cases:** Unanswered required items → confirmation; explanation failure → show correct answers only.

### F10 — Weak-Topic Identification
1. **ID / Name:** F10 Weak-Topic Identification  
2. **Description:** Analyze quiz history to flag weak topics.  
3. **User interaction (GUI):** Progress → Weak Topics.  
4. **Input:** Course ID; history window.  
5. **Output:** Ranked weak topics and suggested review actions.  
6. **AI involvement:** Hybrid (deterministic statistics via `ProgressTracker.weakTopics`; optional LLM narrative).  
7. **Expected workflow:** Aggregate scores by topic → threshold rules → optional LLM summary → display.  
8. **Error / alternative cases:** Insufficient history → request additional quizzes.

### F11 — Progress Tracking and Session History
1. **ID / Name:** F11 Progress Tracking and Session History  
2. **Description:** Record study sessions, quiz scores, and time spent; present history.  
3. **User interaction (GUI):** Progress dashboard and history list.  
4. **Input:** Events from other features; optional filters.  
5. **Output:** Progress lists or charts over time.  
6. **AI involvement:** Deterministic.  
7. **Expected workflow:** `ProgressTracker.record` → notify observers → `ProgressView` renders history.  
8. **Error / alternative cases:** Corrupt log entries are skipped; missing data is reported.

### F12 — Natural-Language Study Commands
1. **ID / Name:** F12 Natural-Language Study Commands  
2. **Description:** Accept goals such as “make a short quiz on Unit 2”; plan and execute tools.  
3. **User interaction (GUI):** Agent chat. **CLI:** `studybuddy ask "…"`.  
4. **Input:** Natural-language utterance and active course context.  
5. **Output:** Plan summary and produced artifacts (for example a quiz).  
6. **AI involvement:** AI / hybrid (planning and tool use).  
7. **Expected workflow:** `AgentController.handleRequest` → `Planner.plan` (may call LLM) → `ToolManager.executeTool` → concrete tools / LLM → aggregate response.  
8. **Error / alternative cases:** Unclear intent → clarifying question; tool failure → partial results with an explicit error.

---

## 3. Design Patterns

| Pattern | Problem addressed | Participating classes | Roles | Rationale | Without the pattern |
|---------|-------------------|----------------------|-------|-----------|---------------------|
| **MVC** | UI and domain logic must evolve independently; GUI and CLI must share behaviour | `StudyGUI`, `StudyCLI`, `AgentController`, domain classes (`Course`, `Material`, `Quiz`, …) | View / Controller / Model | One controller and model serve both interfaces | Duplicated logic or UI-locked domain code |
| **Facade** | Presentation must not wire planner, tools, and LLM directly | `AgentController` | Facade | Single entry API such as `handleRequest()`, `upload()`, `summarize()`, `generateQuiz()`, `ask()` | High coupling from UI to many subsystems |
| **Strategy** | LLM backends must be interchangeable without rewriting tools or UI | `LLMClient`, `OllamaClient` | Strategy interface / concrete strategy | Open for extension (for example a future cloud client) without editing callers | Conditional branches on concrete LLM types throughout the code |
| **Factory Method** | Quiz question objects must be built from LLM output without exposing construction to the UI | `QuizFactory`, `QuizTool`, `Question` | Creator / product | Centralized construction of `Question` objects | UI or tools depend on concrete constructors and parsing details |
| **Observer** | Progress views must refresh when grading records a new attempt | `ProgressTracker`, `ProgressView` | Subject / observer | Loose coupling for live updates after `record()` / `notify()` | Manual UI refresh calls scattered in grading code |

---

## 4. Use-Case Diagram and Descriptions

**Diagram (PNG):** [`diagrams/use_case.png`](diagrams/use_case.png)  
**UMLet source:** [`diagrams/use_case.uxf`](diagrams/use_case.uxf)

### 4.1 Actors
| Actor | Type | Role |
|-------|------|------|
| Student | Primary | Uses GUI and CLI to study |
| Ollama LLM Service | External system | Provides generation and reasoning |
| File System | External system | Supplies uploaded learning files (and may back stored progress data) |

### 4.2 Use cases

| ID | Name | Related features |
|----|------|------------------|
| UC01 | Manage Course | F01 |
| UC02 | Upload Material | F02 |
| UC03 | Summarize Material | F03 |
| UC04 | Extract Concepts | F04 |
| UC05 | Ask Question over Materials | F05 |
| UC06 | Generate Study Plan | F06 |
| UC07 | Generate Flashcards | F07 |
| UC08 | Generate Quiz | F08 |
| UC09 | Take and Grade Quiz | F09 |
| UC10 | Identify Weak Topics | F10 |
| UC11 | View Progress | F11 |
| UC12 | Issue Natural-Language Study Command | F12 |

On the use-case diagram, a dashed arrow labeled `<<include>>` points from UC09 (Take and Grade Quiz) to UC10 (Identify Weak Topics). UC12 may also include quiz generation (UC08) when the natural-language command requests a quiz, as described in the UC12 write-up and SD06.

### 4.3 Use-case descriptions

#### UC01 — Manage Course
- **Actors:** Student  
- **Goal:** Create and maintain course records.  
- **Preconditions:** Application is running.  
- **Trigger:** Student opens the Courses panel and selects New, Edit, or Archive.  
- **Main success scenario:**  
  1. Student enters course details.  
  2. System validates the input.  
  3. System persists the course in `DocumentStore`.  
  4. System refreshes the course list.  
- **Alternative / exception flows:** Invalid or duplicate name → validation message; no persistence.  
- **Postconditions:** Course catalogue reflects the change.  
- **Related features:** F01  

#### UC02 — Upload Material
- **Actors:** Student, File System  
- **Goal:** Import a learning file into a course.  
- **Preconditions:** A course exists.  
- **Trigger:** Student selects Upload Material.  
- **Main success scenario:**  
  1. Student selects a course and file.  
  2. System reads the file from the file system.  
  3. `MaterialImportTool` extracts text.  
  4. System stores a `Material` and confirms success.  
- **Alternative / exception flows:** Unsupported type, empty file, or read failure → error; material not stored.  
- **Postconditions:** Material is available for later agent features.  
- **Related features:** F02  

#### UC03 — Summarize Material
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Produce a summary of a selected material.  
- **Preconditions:** Material text exists; LLM service is configured.  
- **Trigger:** Summarize action or equivalent natural-language request.  
- **Main success scenario:**  
  1. System loads material text.  
  2. Agent invokes summarization via `SummarizeTool` / LLM.  
  3. System stores and displays the summary.  
- **Alternative / exception flows:** Service unavailable → error and retry option; empty material → refuse.  
- **Postconditions:** Summary is stored for the material.  
- **Related features:** F03  

#### UC04 — Extract Concepts
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Extract key concepts from a material.  
- **Preconditions:** Material text exists.  
- **Trigger:** Extract Concepts action.  
- **Main success scenario:** Load material → LLM extraction → parse concepts → store and display.  
- **Alternative / exception flows:** Parse failure → one retry; then show warning with raw output.  
- **Postconditions:** Concept list associated with the course or material.  
- **Related features:** F04  

#### UC05 — Ask Question over Materials
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Answer a question using course materials.  
- **Preconditions:** At least one material exists.  
- **Trigger:** Student submits a question in the Ask panel.  
- **Main success scenario:** `RetrievalTool` retrieves chunks → `PromptBuilder` builds prompt → `OllamaClient` generates answer → display answer with citations.  
- **Alternative / exception flows:** No relevant chunks → report that the answer was not found in materials.  
- **Postconditions:** Answer shown; optional history entry.  
- **Related features:** F05  

#### UC06 — Generate Study Plan
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Create a personalized study plan.  
- **Preconditions:** Course and materials exist.  
- **Trigger:** Generate Study Plan.  
- **Main success scenario:** Collect constraints and progress → plan with LLM support → validate → save → display.  
- **Alternative / exception flows:** Missing date → default horizon; invalid hours → validation error.  
- **Postconditions:** Plan data persisted.  
- **Related features:** F06  

#### UC07 — Generate Flashcards
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Create a flashcard deck from materials or concepts.  
- **Preconditions:** Source material or concepts exist.  
- **Trigger:** Generate Flashcards.  
- **Main success scenario:** Load source → LLM generation → parse deck → save → open review UI.  
- **Alternative / exception flows:** Insufficient content → warning; malformed cards dropped.  
- **Postconditions:** Deck available for review.  
- **Related features:** F07  

#### UC08 — Generate Quiz
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Create a quiz from materials or weak topics.  
- **Preconditions:** Course has usable content.  
- **Trigger:** Generate Quiz.  
- **Main success scenario:** Collect parameters → `QuizTool` / LLM → `QuizFactory` builds questions → open `QuizView`.  
- **Alternative / exception flows:** Invalid structured output → retry or abort with message.  
- **Postconditions:** Quiz ready to take.  
- **Related features:** F08  

#### UC09 — Take and Grade Quiz
- **Actors:** Student, Ollama LLM Service (for explanations)  
- **Goal:** Submit answers and obtain a graded result.  
- **Preconditions:** A quiz exists.  
- **Trigger:** Student submits the quiz.  
- **Main success scenario:** `QuizEngine` grades answers → optional `ExplainTool` explanations → `ProgressTracker` update → display results.  
- **Alternative / exception flows:** Unanswered required items → confirmation; explanation failure → correct answers only.  
- **Postconditions:** Attempt recorded; progress updated. Includes UC10 when history is sufficient.  
- **Related features:** F09, F10, F11  

#### UC10 — Identify Weak Topics
- **Actors:** Student, Ollama LLM Service (optional narrative)  
- **Goal:** Identify topics with weak performance.  
- **Preconditions:** Sufficient quiz history, or invoked after grading.  
- **Trigger:** Weak Topics view, or inclusion from UC09.  
- **Main success scenario:** `ProgressTracker.weakTopics` aggregates scores → apply thresholds → display ranked topics.  
- **Alternative / exception flows:** Insufficient history → request more practice.  
- **Postconditions:** Weak-topic list available for planning and quiz generation.  
- **Related features:** F10  

#### UC11 — View Progress
- **Actors:** Student  
- **Goal:** Review study history and scores.  
- **Preconditions:** None beyond application start.  
- **Trigger:** Open Progress dashboard.  
- **Main success scenario:** Query `ProgressTracker` → `ProgressView` renders history.  
- **Alternative / exception flows:** Missing or corrupt entries skipped with notice.  
- **Postconditions:** Student has viewed current progress.  
- **Related features:** F11  

#### UC12 — Issue Natural-Language Study Command
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Execute a multi-step study task from natural language (GUI or CLI).  
- **Preconditions:** Application running.  
- **Trigger:** Chat submission or CLI `ask` command.  
- **Main success scenario:** Interpret intent → `Planner` produces steps → `ToolManager` executes tools → return artifacts and summary.  
- **Alternative / exception flows:** Unclear intent → clarification; tool failure → partial result with error. May include UC08 when a quiz is requested.  
- **Postconditions:** Planned artifacts created where successful.  
- **Related features:** F12  

---

## 5. Class Diagram

**Diagram (PNG):** [`diagrams/Class.png`](diagrams/Class.png)  
**UMLet source:** [`diagrams/Class.uxf`](diagrams/Class.uxf)

### 5.1 Major classes and interfaces
| Layer | Classes / interfaces |
|-------|----------------------|
| Presentation | `StudyGUI`, `StudyCLI`, `QuizView`, `ProgressView` |
| Control / agent | `AgentController`, `Planner`, `ToolManager` |
| LLM | `LLMClient` (interface), `OllamaClient`, `PromptBuilder` |
| Tools | `Tool` (interface), `SummarizeTool`, `QuizTool`, `MaterialImportTool`, `RetrievalTool`, `ExplainTool`, `QuizFactory` |
| Services | `DocumentStore`, `QuizEngine`, `ProgressTracker` |
| Domain | `Course`, `Material`, `Quiz`, `Question` (`«abstract»`) |

Relationships include associations from `AgentController` to `Planner` and `ToolManager`; realization of `LLMClient` by `OllamaClient` and of `Tool` by the concrete tools; `QuizTool` using `QuizFactory`; composition/aggregation of `Course`–`Material` and `Quiz`–`Question`; and Observer notification from `ProgressTracker` to `ProgressView`. The five design patterns from Section 3 are labeled on the class diagram.

---

## 6. Sequence Diagrams

| ID | Interaction | Features | Diagram (PNG) | UMLet source |
|----|-------------|----------|---------------|--------------|
| SD01 | Upload material | F02 | [`diagrams/Seq_Upload.png`](diagrams/Seq_Upload.png) | [`diagrams/Seq_Upload.uxf`](diagrams/Seq_Upload.uxf) |
| SD02 | Summarize material | F03 | [`diagrams/Seq_Summarize.png`](diagrams/Seq_Summarize.png) | [`diagrams/Seq_Summarize.uxf`](diagrams/Seq_Summarize.uxf) |
| SD03 | Q&A over materials | F05 | [`diagrams/Seq_QA.png`](diagrams/Seq_QA.png) | [`diagrams/Seq_QA.uxf`](diagrams/Seq_QA.uxf) |
| SD04 | Generate quiz | F08 | [`diagrams/Seq_Quiz.png`](diagrams/Seq_Quiz.png) | [`diagrams/Seq_Quiz.uxf`](diagrams/Seq_Quiz.uxf) |
| SD05 | Grade quiz and update progress | F09–F11 | [`diagrams/Seq_Grade.png`](diagrams/Seq_Grade.png) | [`diagrams/Seq_Grade.uxf`](diagrams/Seq_Grade.uxf) |
| SD06 | Natural-language multi-step command | F12 | [`diagrams/Seq_NL_Command.png`](diagrams/Seq_NL_Command.png) | [`diagrams/Seq_NL_Command.uxf`](diagrams/Seq_NL_Command.uxf) |

Participants and method names align with the class diagram. Error paths (for example, LLM unavailable) are described in the feature and use-case sections where they affect control flow.

---

## 7. Feature-to-Design Traceability

| Feature | Description | Type | Use Case | Classes | Key Methods | Sequence | Pattern(s) |
|---------|-------------|------|----------|---------|-------------|----------|------------|
| F01 | Manage courses | Deterministic | UC01 | `StudyGUI`, `AgentController`, `DocumentStore`, `Course` | `create` / persist course via store | — | MVC, Facade |
| F02 | Upload materials | Deterministic | UC02 | `StudyGUI`, `AgentController`, `MaterialImportTool`, `DocumentStore`, `Material` | `upload()`, `extractText()`, `save()` | SD01 | Facade |
| F03 | Summarize | AI | UC03 | `AgentController`, `Planner`, `SummarizeTool`, `OllamaClient`, `DocumentStore` | `summarize()`, `execute()`, `generate()` | SD02 | Facade, Strategy |
| F04 | Extract concepts | AI | UC04 | `AgentController`, `PromptBuilder`, `OllamaClient`, `DocumentStore` | `ask`/`generate` path, store concepts | — | Facade, Strategy |
| F05 | Q&A | Hybrid | UC05 | `AgentController`, `RetrievalTool`, `PromptBuilder`, `OllamaClient` | `ask()`, `retrieve()`, `buildQA()`, `generate()` | SD03 | Facade, Strategy |
| F06 | Study plan | AI / Hybrid | UC06 | `AgentController`, `Planner`, `ProgressTracker`, `OllamaClient`, `DocumentStore` | `plan()`, `generate()`, `weakTopics()` | SD06 | Facade |
| F07 | Flashcards | AI | UC07 | `AgentController`, `OllamaClient`, `DocumentStore` | `handleRequest()` / generate + save | SD06 | Facade, Strategy |
| F08 | Quiz generation | AI | UC08 | `AgentController`, `QuizTool`, `QuizFactory`, `OllamaClient`, `QuizView`, `Quiz` | `generateQuiz()`, `execute()`, `createQuestions()` | SD04 | Factory Method, Strategy, Facade |
| F09 | Grade and explain | Hybrid | UC09 | `QuizView`, `QuizEngine`, `ExplainTool`, `ProgressTracker` | `grade()`, `explain()`, `record()` | SD05 | Observer, Strategy |
| F10 | Weak topics | Hybrid | UC10 | `ProgressTracker`, `ProgressView` | `weakTopics()`, `notify()` | SD05 | Observer |
| F11 | Progress history | Deterministic | UC11 | `ProgressTracker`, `ProgressView` | `record()`, `update()`, `show()` | SD05 | Observer, MVC |
| F12 | NL commands | AI / Hybrid | UC12 | `StudyGUI` / `StudyCLI`, `AgentController`, `Planner`, `ToolManager`, `QuizTool`, `OllamaClient` | `handleRequest()`, `plan()`, `executeTool()` | SD06 | Facade, Strategy |

---

## 8. Feature Realization

### F01 — Manage Courses
`StudyGUI` collects course data and asks `AgentController` to persist it. `DocumentStore` saves a `Course`. No LLM is involved. MVC keeps presentation separate from persistence.

### F02 — Upload Materials
`StudyGUI` obtains a file path and calls `AgentController.upload`. `MaterialImportTool.extractText()` reads content via the file system. `DocumentStore.save(Material)` stores the result and the GUI confirms success (SD01).

### F03 — Summarize
`AgentController.summarize(materialId)` consults `Planner`, then runs `SummarizeTool`. The tool loads material text from `DocumentStore` and calls `OllamaClient.generate` through the `LLMClient` strategy. The summary is saved and returned to the GUI (SD02).

### F04 — Extract Concepts
The agent path loads material, builds an extraction prompt (`PromptBuilder` / LLM), and stores the parsed concept list through `DocumentStore` for later study features.

### F05 — Q&A over Materials
`RetrievalTool` selects relevant chunks. `PromptBuilder.buildQA` constructs the prompt. `OllamaClient` generates the answer. If no suitable chunks exist, the system reports that the answer was not found rather than fabricating sources (SD03).

### F06 — Study Plan
`AgentController` gathers progress from `ProgressTracker` and material context. `Planner` may invoke the LLM. The resulting plan is validated and saved through `DocumentStore`.

### F07 — Flashcards
The LLM produces question/answer pairs that are parsed and stored for review. Natural-language flashcard requests can also travel through the F12 agent path (SD06).

### F08 — Quiz Generation
`QuizTool` obtains structured quiz content from `OllamaClient`. `QuizFactory.createQuestions` constructs `Question` objects. `QuizView` presents the quiz (SD04).

### F09 — Grade and Explain
`QuizEngine.grade` evaluates answers deterministically. `ExplainTool` may call the LLM for explanations of missed items. `ProgressTracker.record` notifies `ProgressView` (SD05).

### F10 — Weak Topics
`ProgressTracker.weakTopics` aggregates attempt statistics. Optional LLM narrative may accompany the ranked list used by planning and quiz generation. UC09 includes this analysis when history is sufficient.

### F11 — Progress History
Session and score events are recorded by `ProgressTracker` and rendered by `ProgressView` via Observer notification.

### F12 — Natural-Language Commands
GUI or CLI text is handled by `AgentController.handleRequest()`. `Planner` produces steps (optionally using the LLM for intent). `ToolManager` executes tools such as `QuizTool`; results are aggregated for the user (SD06). This path demonstrates multi-step agent behaviour.
