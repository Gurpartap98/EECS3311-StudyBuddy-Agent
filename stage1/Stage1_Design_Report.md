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
StudyBuddy Agent is an AI agent-based system with both a graphical user interface (GUI) and a command-line interface (CLI). The agent interprets natural-language study goals, plans multi-step workflows, invokes tools (material import, summarization, quiz generation, grading, progress update), maintains memory of courses and results, and calls a local LLM through Ollama. Deterministic components handle storage, MCQ grading, and progress statistics; the LLM is used for language understanding and generation.

### 1.4 Why an AI Agent Is Appropriate
Study goals are open-ended (e.g., generate a quiz on a weak topic, summarize a week of notes). An agent that selects tools and executes multi-step plans fits this problem better than a single hardcoded flow or a thin chatbot that only forwards prompts to an LLM. The design requires structured domain objects (Course, Material, Quiz, Progress) in addition to model calls.

### 1.5 AI / LLM Model(s)
| Role | Model | Notes |
|------|-------|-------|
| Primary | Ollama `llama3.2:3b` | Local inference; no paid API required for typical workloads |
| Fallback | Ollama `llama3.2:1b` | Used if the 3B model is too slow on the target machine |
| Optional backup | DeepSeek or Gemini API | Used only if local Ollama is unavailable |

The LLM is accessed through an `LLMClient` abstraction. `PromptBuilder` constructs prompts; `ResponseParser` converts model output into domain objects. Runtime path: GUI/CLI → `AgentController` → `Planner` / `ToolManager` → `OllamaClient` → domain objects → UI.

### 1.6 Overall Architecture
The system follows an MVC-style layering with a shared agent facade. `StudyGUI` and `StudyCLI` are presentation components that delegate to `AgentController`. The controller coordinates `Planner`, `MemoryManager`, `ToolManager`, and `LLMClient`. Tools such as `SummarizeTool`, `QuizTool`, and `MaterialImportTool` encapsulate agent capabilities. Persistence and scoring are handled by `DocumentStore`, `QuizEngine`, and `ProgressTracker`. Both interfaces expose the same major features through the shared controller.

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
7. **Expected workflow:** User submits the form; `CourseService` validates; `DocumentStore` persists; UI refreshes.  
8. **Error / alternative cases:** Empty or duplicate name produces a validation message; no change is saved.

### F02 — Upload Learning Materials
1. **ID / Name:** F02 Upload Learning Materials  
2. **Description:** Import PDF, TXT, or Markdown into a course and extract text for later agent use.  
3. **User interaction (GUI):** Course → Upload Material → file picker.  
4. **Input:** File path, course ID, optional title and tags.  
5. **Output:** Stored `Material` with extracted text and metadata.  
6. **AI involvement:** Deterministic for extraction; hybrid if the user pastes text after a parse failure.  
7. **Expected workflow:** Select file → `MaterialImportTool.extractText()` → `DocumentStore.save()` → confirmation.  
8. **Error / alternative cases:** Unsupported type, empty file, or read failure shows an error; material is not added.

### F03 — Document Summarization
1. **ID / Name:** F03 Document Summarization  
2. **Description:** Produce a concise summary of a selected material.  
3. **User interaction (GUI):** Material viewer → Summarize, or natural-language request in chat.  
4. **Input:** Material ID; optional length preference.  
5. **Output:** Summary text, stored and displayed.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** `AgentController` → `Planner` → `SummarizeTool` → `OllamaClient.generate` → store `Summary` → display.  
8. **Error / alternative cases:** Ollama unavailable → error with retry; empty material → refuse summarization.

### F04 — Concept Extraction
1. **ID / Name:** F04 Concept Extraction  
2. **Description:** Extract key concepts from a material into a structured list.  
3. **User interaction (GUI):** Material → Extract Concepts.  
4. **Input:** Material ID.  
5. **Output:** List of concepts (name and short gloss), saved to the course.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** Load material → prompt → parse into `Concept` objects → store → display.  
8. **Error / alternative cases:** Parse failure triggers one retry; persistent failure shows raw output with a warning.

### F05 — Question Answering over Materials
1. **ID / Name:** F05 Q&A over Materials  
2. **Description:** Answer questions using uploaded materials (retrieve relevant chunks, then generate an answer).  
3. **User interaction (GUI):** Ask / Chat panel scoped to a course.  
4. **Input:** Natural-language question; course or material scope.  
5. **Output:** Answer with cited material titles or snippets.  
6. **AI involvement:** Hybrid (deterministic retrieval; AI answer generation).  
7. **Expected workflow:** `RetrievalTool` selects chunks → `PromptBuilder` → `OllamaClient` → display answer and sources.  
8. **Error / alternative cases:** No materials → prompt upload; low relevance → report that the answer was not found in materials.

### F06 — Personalized Study-Plan Generation
1. **ID / Name:** F06 Study-Plan Generation  
2. **Description:** Build a multi-session study plan from materials, deadlines, and weak topics.  
3. **User interaction (GUI):** Study Plan → Generate; form for hours per day and exam date.  
4. **Input:** Course ID, available hours, exam or due date, optional focus topics.  
5. **Output:** `StudyPlan` with ordered sessions and tasks.  
6. **AI involvement:** AI / hybrid (plan validated deterministically).  
7. **Expected workflow:** Gather progress and materials → `Planner` → LLM proposal → validate → save → display.  
8. **Error / alternative cases:** Missing exam date uses a default horizon; invalid hours produce a validation error.

### F07 — Flashcard Generation
1. **ID / Name:** F07 Flashcard Generation  
2. **Description:** Generate question/answer flashcards from a material or concept list.  
3. **User interaction (GUI):** Flashcards → Generate from material.  
4. **Input:** Material or concept set; number of cards.  
5. **Output:** `FlashcardDeck`.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** Load content → LLM → parse cards → save deck → open review UI.  
8. **Error / alternative cases:** Too few concepts → warning; malformed cards are dropped and counted.

### F08 — Quiz Generation
1. **ID / Name:** F08 Quiz Generation  
2. **Description:** Generate a quiz (MCQ or short answer) from materials or weak topics.  
3. **User interaction (GUI):** Quizzes → Generate Quiz.  
4. **Input:** Course or material, question count, difficulty, question type.  
5. **Output:** `Quiz` ready for the student to take.  
6. **AI involvement:** AI-based.  
7. **Expected workflow:** `QuizTool` → LLM → `QuizFactory` builds `Question` objects → open quiz view.  
8. **Error / alternative cases:** Invalid model JSON → retry or repair; otherwise abort with an error message.

### F09 — Automatic Grading and Answer Explanation
1. **ID / Name:** F09 Auto Grading and Explanation  
2. **Description:** Grade a submitted quiz and explain incorrect answers.  
3. **User interaction (GUI):** After Submit → Results and Explain.  
4. **Input:** Quiz attempt answers.  
5. **Output:** Score, per-question feedback, optional explanations.  
6. **AI involvement:** Hybrid (deterministic MCQ grading; AI explanations for open-ended items).  
7. **Expected workflow:** `QuizEngine.grade` → optional `ExplainTool` → `ProgressTracker` update → display results.  
8. **Error / alternative cases:** Unanswered required items → confirmation; explanation failure → show correct answers only.

### F10 — Weak-Topic Identification
1. **ID / Name:** F10 Weak-Topic Identification  
2. **Description:** Analyze quiz and flashcard history to flag weak topics.  
3. **User interaction (GUI):** Progress → Weak Topics.  
4. **Input:** Course ID; history window.  
5. **Output:** Ranked weak topics and suggested review actions.  
6. **AI involvement:** Hybrid (deterministic statistics; optional AI narrative).  
7. **Expected workflow:** Aggregate scores by topic → threshold rules → optional LLM summary → display.  
8. **Error / alternative cases:** Insufficient history → request additional quizzes.

### F11 — Progress Tracking and Session History
1. **ID / Name:** F11 Progress Tracking and Session History  
2. **Description:** Record study sessions, quiz scores, and time spent; present history.  
3. **User interaction (GUI):** Progress dashboard and history list.  
4. **Input:** Events from other features; optional filters.  
5. **Output:** Progress lists or charts over time.  
6. **AI involvement:** Deterministic.  
7. **Expected workflow:** Append to `ProgressTracker` / `SessionLog` → query → render.  
8. **Error / alternative cases:** Corrupt log entries are skipped; missing data is reported.

### F12 — Natural-Language Study Commands
1. **ID / Name:** F12 Natural-Language Study Commands  
2. **Description:** Accept goals such as “make ten flashcards from Unit 2 and a short quiz”; plan and execute tools.  
3. **User interaction (GUI):** Agent chat. **CLI:** `studybuddy ask "…"`.  
4. **Input:** Natural-language utterance and active course context.  
5. **Output:** Plan summary and produced artifacts (plan, quiz, cards).  
6. **AI involvement:** AI / hybrid (planning and tool use).  
7. **Expected workflow:** Interpret intent → `Planner` → `ToolManager` → tools and LLM → aggregate response.  
8. **Error / alternative cases:** Unclear intent → clarifying question; tool failure → partial results with an explicit error.

---

## 3. Design Patterns

| Pattern | Problem addressed | Participating classes | Roles | Rationale | Without the pattern |
|---------|-------------------|----------------------|-------|-----------|---------------------|
| **MVC** | UI and domain logic must evolve independently; GUI and CLI must share behaviour | `StudyGUI`, `StudyCLI`, `AgentController`, domain model classes | View / Controller / Model | One model serves both interfaces | Duplicated logic or UI-locked domain code |
| **Facade** | Presentation must not wire Planner, tools, and LLM directly | `AgentController` | Facade | Single entry API such as `handleRequest()` and `generateQuiz()` | High coupling from UI to many subsystems |
| **Strategy** | LLM backends and quiz styles must be interchangeable | `LLMClient`, `OllamaClient`, `CloudLLMClient`, `QuizGenerationStrategy` | Strategy interface and concrete strategies | Open for extension without editing callers | Conditional branches on concrete types throughout the code |
| **Factory Method** | Question and tool types must be created without exposing concrete classes to the UI | `QuizFactory`, `ToolFactory` | Creator / product | Centralized construction; easy addition of MCQ vs short-answer | UI depends on concrete constructors |
| **Observer** | Progress views must refresh when grading completes | `ProgressTracker`, progress UI panels | Subject / observers | Loose coupling for live updates | Manual UI refresh calls scattered in grading code |

---

## 4. Use-Case Diagram and Descriptions

**Diagram:** [`diagrams/usecase.png`](diagrams/usecase.png)

### 4.1 Actors
| Actor | Type | Role |
|-------|------|------|
| Student | Primary | Uses GUI and CLI to study |
| Ollama LLM Service | External system | Provides generation and reasoning |
| File System | External system | Supplies uploaded learning files |

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

On the use-case diagram, a dashed arrow labeled `<<include>>` points from UC09 (Take and Grade Quiz) to UC10 (Identify Weak Topics). UC12 also includes UC08 (Generate Quiz) as described in the UC12 write-up.

### 4.3 Use-case descriptions

#### UC01 — Manage Course
- **Actors:** Student  
- **Goal:** Create and maintain course records.  
- **Preconditions:** Application is running.  
- **Trigger:** Student opens the Courses panel and selects New, Edit, or Archive.  
- **Main success scenario:**  
  1. Student enters course details.  
  2. System validates the input.  
  3. System persists the course.  
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
  3. System extracts text.  
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
  2. Agent invokes summarization via the LLM.  
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
- **Main success scenario:** Retrieve relevant chunks → build prompt → generate answer → display answer with citations.  
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
- **Postconditions:** `StudyPlan` persisted.  
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
- **Main success scenario:** Collect parameters → LLM generation → factory builds questions → open quiz view.  
- **Alternative / exception flows:** Invalid structured output → retry or abort with message.  
- **Postconditions:** Quiz ready to take.  
- **Related features:** F08  

#### UC09 — Take and Grade Quiz
- **Actors:** Student, Ollama LLM Service (for explanations)  
- **Goal:** Submit answers and obtain a graded result.  
- **Preconditions:** A quiz exists.  
- **Trigger:** Student submits the quiz.  
- **Main success scenario:** Grade answers → optional explanations → update progress → display results.  
- **Alternative / exception flows:** Unanswered required items → confirmation; explanation failure → correct answers only.  
- **Postconditions:** Attempt recorded; progress updated. Includes UC10 when history is sufficient.  
- **Related features:** F09, F10, F11  

#### UC10 — Identify Weak Topics
- **Actors:** Student  
- **Goal:** Identify topics with weak performance.  
- **Preconditions:** Sufficient quiz or flashcard history, or invoked after grading.  
- **Trigger:** Weak Topics view, or inclusion from UC09.  
- **Main success scenario:** Aggregate scores → apply thresholds → display ranked topics.  
- **Alternative / exception flows:** Insufficient history → request more practice.  
- **Postconditions:** Weak-topic list available for planning and quiz generation.  
- **Related features:** F10  

#### UC11 — View Progress
- **Actors:** Student  
- **Goal:** Review study history and scores.  
- **Preconditions:** None beyond application start.  
- **Trigger:** Open Progress dashboard.  
- **Main success scenario:** Query `ProgressTracker` / `SessionLog` → render history.  
- **Alternative / exception flows:** Missing or corrupt entries skipped with notice.  
- **Postconditions:** Student has viewed current progress.  
- **Related features:** F11  

#### UC12 — Issue Natural-Language Study Command
- **Actors:** Student, Ollama LLM Service  
- **Goal:** Execute a multi-step study task from natural language (GUI or CLI).  
- **Preconditions:** Application running.  
- **Trigger:** Chat submission or CLI `ask` command.  
- **Main success scenario:** Interpret intent → plan steps → execute tools → return artifacts and summary.  
- **Alternative / exception flows:** Unclear intent → clarification; tool failure → partial result with error. May include UC08 when a quiz is requested.  
- **Postconditions:** Planned artifacts created where successful.  
- **Related features:** F12  

---

## 5. Class Diagram

**Diagram:** [`diagrams/class.png`](diagrams/class.png)

### 5.1 Major classes and interfaces
| Layer | Classes / interfaces |
|-------|----------------------|
| Presentation | `StudyGUI`, `StudyCLI`, `ChatPanel`, `QuizView`, `ProgressView` |
| Control / agent | `AgentController`, `Planner`, `MemoryManager`, `ToolManager` |
| LLM | `LLMClient` (interface), `OllamaClient`, `CloudLLMClient`, `PromptBuilder`, `ResponseParser` |
| Tools | `Tool` (interface), `SummarizeTool`, `ConceptTool`, `RetrievalTool`, `QuizTool`, `ExplainTool`, `MaterialImportTool` |
| Domain | `Course`, `Material`, `Summary`, `Concept`, `Flashcard`, `FlashcardDeck`, `Quiz`, `Question`, `QuizAttempt`, `StudyPlan`, `StudySession` |
| Services | `DocumentStore`, `CourseService`, `QuizEngine`, `QuizFactory`, `ToolFactory`, `ProgressTracker`, `SessionLog` |

Relationships include associations from `AgentController` to planner, memory, and tools; realization of `LLMClient` and `Tool`; composition of `Quiz` with `Question`; and aggregation of `Course` with `Material`. Design patterns from Section 3 are indicated on the class diagram.

---

## 6. Sequence Diagrams

| ID | Interaction | Features | Diagram |
|----|-------------|----------|---------|
| SD01 | Upload material | F02 | [`diagrams/seq_upload.png`](diagrams/seq_upload.png) |
| SD02 | Summarize material | F03 | [`diagrams/seq_summarize.png`](diagrams/seq_summarize.png) |
| SD03 | Q&A over materials | F05 | [`diagrams/seq_qa.png`](diagrams/seq_qa.png) |
| SD04 | Generate quiz | F08 | [`diagrams/seq_quiz.png`](diagrams/seq_quiz.png) |
| SD05 | Grade quiz and update progress | F09–F11 | [`diagrams/seq_grade.png`](diagrams/seq_grade.png) |
| SD06 | Natural-language multi-step command | F12 | [`diagrams/seq_nl_command.png`](diagrams/seq_nl_command.png) |

Participants and method names align with the class diagram. Error paths (for example, LLM unavailable) are shown where they affect control flow.

---

## 7. Feature-to-Design Traceability

| Feature | Description | Type | Use Case | Classes | Key Methods | Sequence | Pattern(s) |
|---------|-------------|------|----------|---------|-------------|----------|------------|
| F01 | Manage courses | Deterministic | UC01 | `StudyGUI`, `CourseService`, `DocumentStore`, `Course` | `createCourse()`, `listCourses()` | — | MVC |
| F02 | Upload materials | Deterministic | UC02 | `StudyGUI`, `MaterialImportTool`, `DocumentStore`, `Material` | `upload()`, `extractText()`, `save()` | SD01 | Facade, Factory |
| F03 | Summarize | AI | UC03 | `AgentController`, `Planner`, `SummarizeTool`, `OllamaClient`, `Material` | `summarize()`, `generate()` | SD02 | Facade, Strategy |
| F04 | Extract concepts | AI | UC04 | `AgentController`, `ConceptTool`, `OllamaClient`, `Concept` | `extractConcepts()`, `parse()` | SD02 | Strategy, Factory |
| F05 | Q&A | Hybrid | UC05 | `AgentController`, `RetrievalTool`, `PromptBuilder`, `OllamaClient` | `ask()`, `retrieve()`, `generate()` | SD03 | Facade, Strategy |
| F06 | Study plan | AI / Hybrid | UC06 | `AgentController`, `Planner`, `ProgressTracker`, `OllamaClient`, `StudyPlan` | `createPlan()`, `generate()` | SD06 | Facade |
| F07 | Flashcards | AI | UC07 | `AgentController`, `FlashcardTool`, `FlashcardDeck`, `OllamaClient` | `generateDeck()` | SD06 | Factory, Strategy |
| F08 | Quiz generation | AI | UC08 | `AgentController`, `QuizTool`, `QuizFactory`, `OllamaClient`, `Quiz` | `generateQuiz()`, `createQuestions()` | SD04 | Factory, Strategy, Facade |
| F09 | Grade and explain | Hybrid | UC09 | `QuizView`, `QuizEngine`, `ExplainTool`, `ProgressTracker` | `grade()`, `explain()` | SD05 | Observer, Strategy |
| F10 | Weak topics | Hybrid | UC10 | `ProgressTracker`, `AgentController`, `OllamaClient` | `identifyWeakTopics()` | SD05 | Observer |
| F11 | Progress history | Deterministic | UC11 | `ProgressTracker`, `SessionLog`, `ProgressView` | `record()`, `history()` | SD05 | Observer, MVC |
| F12 | NL commands | AI / Hybrid | UC12 | `StudyGUI` / `StudyCLI`, `AgentController`, `Planner`, `ToolManager`, `LLMClient` | `handleRequest()`, `plan()`, `executeTool()` | SD06 | Facade, Strategy |

---

## 8. Feature Realization

### F01 — Manage Courses
`StudyGUI` collects course data and invokes `CourseService.createCourse()`. After validation, `DocumentStore` persists a `Course`. No LLM is involved. MVC keeps presentation separate from persistence.

### F02 — Upload Materials
`StudyGUI` obtains a file path. `MaterialImportTool.extractText()` reads content via the file system. `DocumentStore.save(Material)` stores the result and the GUI confirms success.

### F03 — Summarize
`AgentController.summarize(materialId)` delegates to `Planner`, which selects `SummarizeTool`. The tool loads material text and calls `OllamaClient.generate` through the `LLMClient` strategy. The resulting `Summary` is returned to the GUI.

### F04 — Extract Concepts
The same agent path uses `ConceptTool`. `ResponseParser` builds `Concept` objects that are stored on the course.

### F05 — Q&A over Materials
`RetrievalTool` selects relevant chunks. `PromptBuilder` constructs the prompt. `OllamaClient` generates the answer. If no suitable chunks exist, the system reports that the answer was not found rather than fabricating sources.

### F06 — Study Plan
`AgentController` gathers progress and constraints. `Planner` may invoke tools and the LLM. The resulting `StudyPlan` is validated and saved.

### F07 — Flashcards
The LLM produces pairs that are parsed into a `FlashcardDeck` and stored for review.

### F08 — Quiz Generation
`QuizTool` obtains structured quiz content from the LLM. `QuizFactory` constructs `Question` subtypes according to the selected strategy. The quiz view presents the result.

### F09 — Grade and Explain
`QuizEngine.grade` evaluates MCQs deterministically. `ExplainTool` may call the LLM for open-ended items. `ProgressTracker` notifies observers so progress views refresh.

### F10 — Weak Topics
`ProgressTracker` aggregates attempt statistics. Optional LLM narrative may accompany the ranked list used by planning and quiz generation.

### F11 — Progress History
Session and score events are appended to `ProgressTracker` / `SessionLog` and rendered in `ProgressView`.

### F12 — Natural-Language Commands
GUI or CLI text is handled by `AgentController.handleRequest()`. `Planner` produces steps; `ToolManager` executes tools; results are aggregated for the user. This path demonstrates multi-step agent behaviour and may include quiz generation (UC08).
