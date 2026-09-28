# StudyForge AI - Phase 0 Implementation Plan

Status: Phase 0 design baseline; implementation has not started.

## 1. Course-requirement checklist

| Requirement | Planned evidence |
|---|---|
| Web GUI | Spring MVC controllers and Thymeleaf pages |
| CLI | Picocli commands calling the application layer |
| 10+ meaningful features | F01-F14 |
| 5+ patterns | MVC, Facade, Adapter, Strategy, Command, Observer, Repository |
| LLM | `LlmClient` with `OllamaLlmAdapter` |
| Genuine agent | intent analysis, planning, validation, whitelisted tools, execution, memory |
| UML | PlantUML class, use-case, and sequence diagrams |
| Traceability | Stage 1 feature-to-use-case/class/method/test mapping |
| Deterministic tests | JUnit 5 tests for processing, retrieval, grading, progress, planning |
| Behavioral readiness | requirements and scenarios for later KUMA testing; no fabricated KUMA results |

No `stage1.pdf`, `Project-Instruction.pdf`, or other project PDF exists in the repository at Phase 0. If one is added later, it supersedes this plan where the two conflict.

## 2. Final architecture

The system is a modular monolith with clear boundaries:

`Web/CLI -> StudyAgentFacade or shared application service -> domain services/repositories/tools -> persistence/infrastructure`

The agent path is:

`StudyAgentFacade -> AgentCoordinator -> IntentAnalyzer -> TaskPlanner -> AgentPlanValidator -> AgentExecutionEngine -> ToolManager -> whitelisted AgentCommand -> application services -> PromptBuilder/LlmClient -> validated domain result`

For important end-user workflows, both web controllers and CLI commands call `StudyAgentFacade`, which delegates to `AgentCoordinator` and application services. Simple CRUD may call a shared application service directly when no agent orchestration is needed. Neither interface contains business logic or a duplicate workflow.

## 3. Package structure

```text
ca.yorku.eecs3311.coursepilot
  CoursePilotApplication
  config
  domain.model
  domain.enums
  domain.event
  domain.repository
  application
  application.dto
  application.service
  agent
  agent.command
  agent.tool
  agent.validation
  infrastructure.persistence
  infrastructure.document
  infrastructure.retrieval
  infrastructure.llm
  web
  web.viewmodel
  cli
```

## 4. Domain entities and relationships

Core entities: `Course`, `CourseDocument`, `DocumentChunk`, `Concept`, `Flashcard`, `FlashcardAttempt`, `Quiz`, `QuizQuestion`, `QuestionResponse`, `QuizAttempt`, `TopicProgress`, `StudyPlan`, `StudyActivity`, and `LearningSession`.

`Course` owns documents, concepts, quizzes, flashcards, progress, plans, and learning history. A document owns chunks. Generated activities retain source document/chunk references where applicable. Quiz completion produces an event consumed by progress and adaptation listeners.

## 5. Repositories

Spring Data JPA repositories are planned for each aggregate that needs persistence: course, document, chunk, concept, flashcard, quiz, attempt, progress, plan, and session. Query methods will enforce course scope and avoid loading large entity graphs into web views.

## 6. Application services

Planned services include `CourseService`, `MaterialImportService`, `MaterialSearchService`, `QuestionAnsweringService`, `SummarizationService`, `ConceptService`, `FlashcardService`, `QuizService`, `ProgressService`, `StudyPlanService`, `RecommendationService`, and `LearningHistoryService`.

## 7. Agent classes

`StudyAgentFacade`, `AgentCoordinator`, `IntentAnalyzer`, `TaskPlanner`, `AgentPlanValidator`, `AgentExecutionEngine`, `AgentContext`, `AgentPlan`, `AgentPlanStep`, `AgentToolManager`, `PromptBuilder`, and `StructuredResponseParser` coordinate observable multi-step workflows. The model can suggest intent/structured parameters, but deterministic validation and tool access remain authoritative.

## 8. Tools and commands

Tools expose narrow capabilities: `CourseSearchTool`, `ProgressTool`, `HistoryTool`, `QuizGenerationTool`, `StudyPlanTool`, `ConceptTool`, and `RecommendationTool`. Commands implement real actions such as retrieve material, load progress, generate quiz, and create plan. A registry maps fixed plan-step names to commands; model output cannot instantiate arbitrary classes.

## 9. Design patterns and exact participants

| Pattern | Participants | Problem solved |
|---|---|---|
| MVC | Spring controllers, Thymeleaf views, application services | Separates web presentation from behavior |
| Facade | `StudyAgentFacade` | Gives GUI and CLI one stable agent entry point |
| Adapter | `LlmClient`, `OllamaLlmAdapter` | Isolates provider HTTP details |
| Strategy | `StudyStrategy`, balanced/weak-topic/exam-cram strategies | Varies planning policy without changing orchestration |
| Command | `AgentCommand`, concrete tool commands, command registry | Executes validated multi-step actions uniformly |
| Observer | `QuizCompletedEvent`, progress/adaptive listeners | Decouples quiz completion from downstream updates |
| Repository | Spring Data repositories | Isolates persistence from domain/application logic |

## 10. Web controllers and pages

Planned controllers: `CourseController`, `MaterialController`, `StudyController`, `QuizController`, `FlashcardController`, `ProgressController`, and `AgentController`.

Planned pages: course list/form/dashboard, material import/search, question/answer, summaries, concepts, flashcards/review, quiz generation/taking/results, progress, study plans, recommendations, and history. Server-rendered Thymeleaf and semantic HTML are the baseline; JavaScript is optional and minimal.

## 11. CLI commands

Picocli commands will include `course list/create/show`, `material search/import`, `ask`, `summarize`, `quiz generate/take`, `flashcard generate/review`, `progress show`, `plan create`, `recommend`, and `history list`. Commands translate input to DTOs and call shared application services/facade.

## 12. LLM integration

`LlmClient` returns a typed `LlmResponse` from an `LlmRequest`. `OllamaLlmAdapter` uses Spring `RestClient`; URL/model/provider are configuration properties with environment overrides. `PromptBuilder` creates grounded, bounded prompts. Ollama outages become controlled application errors; deterministic features remain available. Structured outputs are parsed with Jackson, validated against DTO constraints, checked against retrieved source IDs, and retried once only where specified.

## 13. Retrieval approach

PDFBox extracts PDF text with page metadata; UTF-8 handles TXT/MD. Text is normalized and chunked. Search uses deterministic token overlap, exact-phrase bonus, heading bonus, and filename/title bonus. Results carry document/chunk/page/source information. No embeddings or external search are required in version 1.

## 14. Error handling

Validation errors return inline web messages or concise CLI errors. Missing courses/material return friendly not-found or insufficient-material results. Unsupported/oversized/corrupt uploads fail without appearing completed. AI failures are logged technically but shown as a safe message. Plan validation rejects unknown tools, invalid parameters, cross-course access, and excessive steps.

## 15. Testing approach

Unit tests cover normalization, chunking, retrieval scoring, grading, progress, strategy differences, plan validation, and command mapping. Integration tests cover JPA repositories, import flow, event publication/listeners, MVC routes, and CLI wiring using isolated H2. LLM tests use a test double only in tests. Later behavioral tests will assert observable agent behavior, not hidden reasoning.

## 16. UML plan

PlantUML sources are under `docs/stage1/uml/`: class diagram, use-case diagram, and seven behavior sequences: SD01 import, SD02 grounded Q&A, SD03 topic quiz generation, SD04 quiz completion/progress, SD05 study plan, SD06 adaptive recommendation, and SD07 flashcards. Diagram method names are intended to become implementation contracts and must be updated if the design changes.

## 17. Feature-to-class mapping

See `docs/stage1/05-feature-traceability.md` and `docs/stage1/06-feature-realization.md`. Every F01-F14 is assigned a use case, application service, important methods, pattern participation, and planned sequence coverage.

## 18. Implementation phases

1. Phase 0: repository inspection, source-of-truth check, design baseline, self-audit.
2. Phase 1: Maven/Spring skeleton, configuration, domain entities, repositories, migrations/schema startup.
3. Phase 2: course management and material import/processing.
4. Phase 3: deterministic retrieval and web/CLI search.
5. Phase 4: LLM adapter, structured validation, grounded Q&A and hybrid features.
6. Phase 5: quizzes, flashcards, grading, progress, events, and history.
7. Phase 6: planning strategies, adaptive recommendations, full agent orchestration.
8. Phase 7: integration testing, GUI/CLI demonstration, documentation synchronization, self-audit.

## 19. Risks

Primary risks are Ollama availability/model differences, malformed structured output, PDF extraction variability, scope growth, UML/code drift, and AI-agent behavioral failures. Mitigations are deterministic fallbacks where appropriate, strict DTO validation, bounded prompts, source-ID validation, isolated tests, behavioral requirements, and a traceability audit after each phase.

## 20. Scope exclusions

Authentication, external LMS/calendar/email integrations, OCR, speech, vector databases, cloud deployment, microservices, distributed infrastructure, and mobile apps are explicitly excluded from the initial project.

## Phase 0 self-audit

The proposed design supports GUI, CLI, F01-F14, six or more meaningful patterns, LLM integration, retrieval, planning, tool execution, memory/history, multi-step workflows, deterministic testing, future KUMA testing, UML, and traceability. No blocking architectural contradiction was found. Implementation remains intentionally paused pending approval of this baseline.
