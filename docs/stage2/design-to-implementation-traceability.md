# Stage 2 - Design-to-Implementation Traceability

| Designed feature/use case | Implemented classes/files | Status |
|---|---|---|
| F01 / UC01 Course Management | `Course`, `CourseRepository`, `CourseService`, `StudyAgentFacade`, `CourseController`, dashboard/course Thymeleaf templates, Picocli course commands | Implemented + verified |
| F02 / UC02 Course Material Import and Processing | `MaterialController`, `CourseMaterialService`, `StudyAgentFacade`, `CourseDocument`, `DocumentChunk`, `DocumentValidator`, `HashService`, `FileStorageService`, extractor implementations, `TextNormalizer`, `TextChunker`, document repositories, materials templates, material Picocli commands | Implemented + verified |
| F03 / UC03 Course Material Search and Retrieval | `MaterialController`, `MaterialSearchCommand`, `CourseSearchService`, `QueryNormalizer`, `RetrievalScorer`, `LexicalRetrievalScorer`, `DocumentChunkRepository`, `SearchResult`, materials search template | Implemented + verified |
| MVC | `CourseController`, `DashboardController`, templates, `CourseService` | Implemented for F01 |
| Facade | `StudyAgentFacade` delegates course operations to `CourseService` | Implemented for F01 |
| Repository | `CourseRepository` and H2 persistence | Implemented for F01 |
| CLI shared logic | `CoursePilotCliApplication`, `CliRunner`, course commands -> `StudyAgentFacade` -> `CourseService` | Implemented + verified |
| F02 processing pipeline | `CourseMaterialService` -> validator/hash/storage/extractor/normalizer/chunker -> document/chunk repositories | Implemented + verified |
| F03 retrieval pipeline | `CourseSearchService` -> course/status-scoped chunk query -> `QueryNormalizer` -> `LexicalRetrievalScorer` -> threshold/top-K -> `SearchResult` | Implemented + verified |

## Phase 4 foundation traceability

| Scope | Design responsibility | Realization | Status |
|---|---|---|---|
| AI foundation | Provider-neutral LLM contract and typed exchange | `LlmClient`, `LlmRequest`, `LlmResponse`, `LlmAvailability` | Implemented + verified |
| Ollama adapter | Isolate provider HTTP protocol behind Adapter pattern | `OllamaLlmAdapter` | Implemented + verified |
| AI configuration | Runtime-selectable provider URL/model/timeouts | `AiProperties`, `application.properties` | Implemented + verified |
| Prompt/structured output utilities | Reusable prompt sections and fenced JSON parsing | `PromptBuilder`, `StructuredResponseParser` | Implemented + verified |
| Operational status | Safe availability inspection through CLI | `AiStatusCommand`, `StudyAgentFacade.aiStatus()` | Implemented + verified |

## Phase 5 F04 traceability

| Scope | Design responsibility | Realization | Status |
|---|---|---|---|
| Grounded Q&A workflow | Retrieval-first application orchestration | `GroundedQuestionService` | Implemented + verified |
| Grounding context | Stable source IDs, bounded context, source metadata | `GroundingContextBuilder`, `GroundedSource` | Implemented + verified |
| Provider-neutral generation | Structured grounded request through existing AI boundary | `PromptBuilder.buildGroundedQuestionPrompt()`, `LlmClient` | Implemented + verified |
| Safe result mapping | Validate model source IDs and expose provider-independent answer | `GroundedAnswerLlmResponse`, `GroundedAnswer` | Implemented + verified |
| Web Q&A | Course-scoped ask form and answer/source display | `AskController`, `templates/ask/form.html` | Implemented + verified |
| CLI Q&A | Shared application-service command | `AskCommand` | Implemented + verified |

The Stage 1 UML abbreviated the service contract as `findByCode`; Phase 1 uses the clearer `getCourseByCode` and DTO-returning methods (`getAllCourses`, `getCourse`, `updateCourse`, `deleteCourse`). The class diagram has been updated to match.

Future features remain designed but not implemented. This table will be expanded after each approved phase.
