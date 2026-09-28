# Stage 2 - Design Changes

Phase 1 implementation introduced no conceptual deviation from the approved architecture. The CLI uses a separate `CoursePilotCliApplication` entry point with a `cli` Spring profile so the web app does not start unnecessarily; both entry points still call `StudyAgentFacade` and `CourseService`. This is an implementation detail of the approved GUI/CLI sharing rule, not a feature redesign.

The Stage 1 UML abbreviated the course lookup method as `findByCode`. During implementation it was clarified as `getCourseByCode`, and the service now returns DTOs at the application boundary. The UML and Stage 2 traceability were updated; domain persistence remains behind the service.

Verification corrections did not change the approved architecture: duplicate detection was made null-safe for new entities and its test fixture now supplies a persisted ID; web forms explicitly bind to the template's `courseForm` model so validation messages render; and the README CLI example was corrected for Windows PowerShell's `-D` argument parsing.

For F02, the approved Stage 1 `MaterialImportService`/`FileStorage` names were realized as `CourseMaterialService`/`FileStorageService` to match the existing Phase 1 service naming convention. The workflow and SD01 responsibilities remain the same; the Stage 1 traceability and UML were updated to the actual names. No intentional technology-stack or feature-scope deviation was introduced.

For F03, the planned `MaterialSearchService` name was realized as `CourseSearchService` because it searches one selected course and follows the existing `CourseService` naming convention. The implementation uses one concrete lexical scorer behind the `RetrievalScorer` abstraction; no artificial alternative algorithms were added merely to claim a design pattern. The SD08 sequence and traceability now reflect the actual classes.

When implementation changes the Stage 1 design, record the original design, reason, affected files/classes/UML, human decision, and verification evidence here rather than silently rewriting Stage 1 history.

## Phase 4

- Realized the planned provider-neutral `LlmClient` plus `OllamaLlmAdapter` boundary without introducing a framework-specific AI SDK.
- Added typed AI contracts, configurable `AiProperties`, prompt/structured-output utilities, and an explicit CLI status capability.
- No deviation from the planned scope: Q&A, embeddings, OCR, vector search, and agent orchestration remain future work.

## Phase 5

- Realized F04 through a dedicated `GroundedQuestionService`; presentation layers delegate the complete workflow rather than duplicating retrieval or prompting.
- Source content uses existing F03 snippets because the provider-neutral Q&A contract needs bounded, inspectable context; no repository-wide query or second retrieval algorithm was introduced.
- Invalid model source references are rejected as an unsafe response rather than displayed.

## Phase 7

F07 follows the Stage 1 persistence intent by adding normalized `Flashcard`, `FlashcardSource`, and `FlashcardReview` tables. Review history is intentionally flashcard-specific and append-only; global learning history, progress calculations, spaced repetition, and event orchestration remain deferred. Source provenance is relational rather than a fabricated text-only field.
