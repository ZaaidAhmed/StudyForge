# Stage 2 Design-to-Implementation Compliance Crosswalk

This is the implementation crosswalk for the Stage 1 proposal and Stage 2 requirements. The original proposal remains the baseline; F15 is new in Stage 2. For detailed explanations, see `stage2-report.md` Sections 2-5. A status of **implemented** describes source behavior, not proof that a public video link is accessible.

| Feature | Stage 1 proposal / use case | Implemented behavior and responsibility | Key implementation | Type | GUI / CLI | Verification evidence | Demo |
|---|---|---|---|---|---|---|---|
| F01 Course workspace | Original / UC01 | Create, view, update, delete course; validate code and course scope. | `CourseController`; `CourseService` (`createCourse`, `getCourseByCode`, `updateCourse`, `deleteCourse`); `CourseRepository`; `StudyAgentFacade`; Picocli course commands. | Deterministic | GUI; CLI create/list/show | Course service/repository/controller and CLI tests. | [F01](https://youtu.be/Uq1Muw7ICKc?t=0) |
| F02 Material import and processing | Original / UC02 | Validate supported file, safely store, extract, normalize, chunk, persist. | `MaterialController`; `CourseMaterialService.importMaterial`; validators, storage, extractors, normalizer, chunker, document/chunk repositories. | Deterministic | GUI and CLI add/list/show | Material service, document processing, controller, CLI tests. | [F02](https://youtu.be/Y1sINf8Ev6M?t=0) |
| F03 Material retrieval | Original / UC03 | Search READY course chunks with explainable lexical scores and provenance. | `CourseSearchService.search`; `QueryNormalizer`; `RetrievalScorer`; `LexicalRetrievalScorer`; web controller and CLI command. | Deterministic | GUI and CLI search | Search/scorer and CLI tests. | [F03](https://youtu.be/EhUPZO6aWq4?t=0) |
| F04 Grounded Q&A | Original / UC04 | Retrieve bounded course/document evidence, generate answer, validate source references; return controlled insufficiency/failure. | `GroundedQuestionService.answer`; `GroundingContextBuilder`; `PromptBuilder`; `LlmClient`; Ask controller/command. | Hybrid | GUI and CLI | Grounded service and MVC tests. | [F04](https://youtu.be/i9FBAiej5VI?t=0) |
| F05 Summaries | Original / UC05 | Summarize one document or a topic using bounded evidence and validated sources. | `SummarizationService.summarizeDocument/summarizeTopic`; Learning controller; `SummaryCommand`. | Hybrid | GUI and CLI | Summarization service tests. | [F05](https://youtu.be/XH3T5Gb1PJk?t=0) |
| F06 Concepts | Original / UC06 | Extract structured concepts, validate source IDs, merge duplicate terms. | `ConceptExtractionService.extract`; parser; Learning controller; `ConceptsCommand`. | Hybrid | GUI and CLI | Concept extraction/parser tests. | [F06](https://youtu.be/o8mz4fHb54Q?t=0) |
| F07 Flashcards | Original / UC07 | Generate document-scoped cards with source metadata; persist; record review ratings. | `FlashcardGenerationService.generate`; `FlashcardReviewService.review/list`; flashcard entities/repos; GUI and Picocli commands. | Hybrid generation; deterministic review | GUI and CLI | Generation, review, MVC, CLI tests. | [F07](https://youtu.be/gLEU1C3nofI?t=0) |
| F08 Quiz generation | Original / UC08 | Generate, validate, deduplicate, recover, and persist sourced quiz questions. | `QuizGenerationService.generate`; `QuizController`; `QuizCommand.Generate/List`; quiz/source repositories. | Hybrid | GUI and CLI generation/list | Quiz generation, controller, CLI tests. | [F08](https://youtu.be/NOIpg61Zin0?t=0) |
| F09 Quiz taking/grading | Original / UC09 | Persist attempt/responses; objective questions graded deterministically; legacy short answers use source-checked AI with deterministic fallback. | `QuizAttemptService.submit`; `QuizController`; attempt/response entities/repos. | Hybrid | GUI only | Attempt service and quiz MVC tests. | [F09](https://youtu.be/5k17vhDafk4?t=0) |
| F10 Wrong-answer explanation | Original / UC10 | Validate course/quiz/question/attempt and sources; provide grounded teaching explanation without changing saved grade. | `WrongAnswerExplanationService.explain`; `GroundedExplanation`; quiz result UI; `LlmClient`. | Hybrid | GUI only | Explanation service and quiz MVC tests. | [F10](https://youtu.be/m-_c432u1XY?t=0) |
| F11 Progress and weak topics | Original / UC11 | Aggregate persisted ratings and quiz outcomes into deterministic course/topic signals. | `ProgressService.summarize`; `ProgressController`; `ProgressCommand`; `ProgressSummary`. | Deterministic | GUI and CLI | Progress service/controller/CLI tests. | [F11](https://youtu.be/Kk_OJeXuBHU?t=0) |
| F12 Study plans | Original / UC12 | Validate total budget/date range, apply selected strategy, persist dated sessions. | `StudyPlanService.create/list`; `StudyPlanningStrategy`; three strategy implementations; controller/command/repos. | Deterministic | GUI and CLI | Study-plan service/strategy/MVC/CLI tests. | [F12](https://youtu.be/XrigYJkYvk8?t=0) |
| F13 Recommendations and agent | Original / UC13 | Deterministic next action plus bounded intent/planning/validation/allowlisted tools and ten-event memory. | `RecommendationService`; `AgentCoordinator`; `IntentAnalyzer`; `TaskPlanner`; `AgentPlanValidator`; `AgentToolManager`; `MemoryManager`; response synthesizer; CLI commands. | Hybrid | GUI and CLI | Recommendation, planning, tool manager, behavioral, and MVC tests. | [F13](https://youtu.be/ATYw-0oPk8A?t=0) |
| F14 History and bounded memory | Original / UC14 | Derive newest-first course activity from reviews, attempts, plans; page/CLI cap 100; agent memory cap 10. | `LearningHistoryService`; `HistoryEvent`; `MemoryManager.recent`; history controller/command. | Deterministic | GUI and CLI | History service/controller/CLI tests. | [F14](https://youtu.be/91A3CTHtKxI?t=0) |
| F15 Provider selection | New Stage 2 addition | GUI applies supported provider/model at runtime; CLI reads provider/model from environment; status command reports active selection. | `AiStatusController`; `AiProperties`; `RoutingLlmClient`; `OllamaLlmAdapter`; `GeminiLlmAdapter`; `AiStatusCommand`. | Deterministic routing; AI calls remain provider-dependent | GUI and CLI environment configuration/status; no CLI switch subcommand | Provider adapter and routing/configuration tests. | [F15](https://youtu.be/LKDUHKkV1Gs?t=0) |

## Design pattern crosswalk

| Pattern | Stage 1 correspondence | Implemented participants | Design problem solved |
|---|---|---|---|
| MVC | Proposed | Spring MVC controllers, application services, Thymeleaf templates | Separates HTTP/presentation from reusable behavior. |
| Facade | Proposed | `StudyAgentFacade`, web controllers, CLI commands, services/coordinator | Provides one application boundary for multiple interfaces. |
| Adapter | Proposed Ollama adapter; expanded | `LlmClient`, Ollama/Gemini adapters, `RoutingLlmClient` | Isolates provider wire protocols and provider choice. |
| Strategy | Proposed | `StudyPlanningStrategy`, balanced/weak-topic/exam strategies, `StudyPlanService` | Varies schedule allocation without branching the service around every policy. |
| Command | Proposed Picocli commands | Picocli invoker and concrete `*Command` classes | Encapsulates parsed user operations and delegates to shared services. |
| Repository (supplementary) | Proposed persistence abstraction | Spring Data repositories | Isolates persistence queries and ownership scopes. |

## Stage 2 deviations and additions

| Original design | Change | Reason | Impact |
|---|---|---|---|
| Ollama-only provider in proposal | Add Gemini and runtime provider routing; create F15 | Support local/cloud choice while retaining a common client contract. | Existing grounded services remain provider-neutral; secrets stay local. |
| `MaterialImportService`, `MaterialSearchService` names | `CourseMaterialService`, `CourseSearchService` | Consistent service naming and explicit course scope. | Same use cases and data flow. |
| Model generation with bounded response handling | Add schema aliases, validation, duplicate rejection, retries/backfill, explicit partial results. | Recover from provider variability while guarding persistence quality. | Request counts are targets where grounded unique content permits; insufficient content is reported. |
| Original document display | Application-controlled document reader for chat | Integrate reading and document Q&A in the workspace. | Preserves document-scoped learning interaction. |
| Basic quiz deletion | Delete dependent attempts first within course ownership boundary | Avoid orphaned attempt data. | Existing quiz deletion remains course scoped. |
| Basic F14 timeline/memory | Group/filter history; use only ten recent events selectively for relevant agent requests. | Improve scanability and bound contextual memory. | Excludes transcripts and hidden model reasoning. |

## Evidence caveats

- Demo URLs are in the published index; public accessibility and exact timestamps still require unauthenticated playback verification. `t=0` points to the start of the feature video, not a verified later segment boundary.
- The public index currently lists F08/F13 CLI evidence as pending. Their commands are implemented, but the public feature-specific clips must be checked or supplemented before claiming CLI video proof for them.
- The Stage 2 automated suite is not the Stage 3 KUMA agent-validation deliverable.
- Test/run statuses in this file must be updated only from the final release run, not historical build notes.
