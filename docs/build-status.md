# Build Status

## Phase 0 / Phase 0.5

Status: DESIGN-ONLY / IMPLEMENTATION NOT STARTED

- Repository inspection completed: workspace was empty.
- Initial course-document inspection completed before the PDFs were added: no PDF was present then.
- Phase 0.5 reconciliation completed against `docs/reference/stage1.pdf` and `docs/reference/Project-Instruction.pdf`.
- `docs/implementation-plan.md` created.
- Initial Stage 1 design documentation and PlantUML sources created and reconciled.
- Feature specification audit, Stage 1 compliance checklist, and behavioral requirements created.
- Stage 2 collaboration-log and design-change templates created without fabricated entries.
- No Maven project exists yet, so no compile or test command was run.

Phase 0 gate: approved by the user; Phase 1 implementation has now been started/completed in source.

## Phase 1

Status: IMPLEMENTED + VERIFIED

- Maven project and Java 21 configuration created.
- F01 course domain, persistence, service, facade, web GUI, and CLI source created.
- Tests created for service behavior, repository persistence, and MVC behavior.
- Java and Maven are installed in the supplied download directories, nested one level below their extraction folders. They were verified without changing global Windows environment variables.
- Java: OpenJDK `21.0.12.1`, `javac 21.0.12.1`; Maven Daemon `mvnd 1.0.6`; bundled Apache Maven `3.9.16`.
- `mvnd clean test`: PASS, 8 tests, 0 failures, 0 errors, 0 skipped.
- `mvnd clean package`: PASS; executable JAR created at `target/studyforge-ai-0.1.0-SNAPSHOT.jar`.
- Packaged application startup: PASS on port 8080.
- Web checks: dashboard, course list, and course form returned HTTP 200; create, duplicate-code validation, blank-field validation, and persistence across restart passed.
- CLI checks: list read web-created `EECS3311`; create added `EECS3101`; list and show returned it; duplicate create and missing-course show returned exit code 1.
- Cross-interface persistence: after CLI creation, the restarted web application listed both `EECS3311` and `EECS3101`.
- Verification corrected a CLI lambda compilation issue, a null-id test fixture issue, and a validation-model binding issue. These are implementation corrections, not Phase 2 work.
- Remaining build output consists of non-blocking warnings about deprecated `@MockBean` usage and Mockito's dynamic-agent notice.
- Phase 2 features remain not started.

## Phase 2

Status: IMPLEMENTED + VERIFIED

- F02 document pipeline implemented using Spring Data JPA/H2, Apache PDFBox, and the existing MVC/Facade/Picocli architecture.
- Supported and verified: PDF, UTF-8 TXT, and UTF-8 Markdown upload/import; extension/empty/size/path validation; SHA-256; same-course duplicate detection; generated safe storage names; text extraction; deterministic normalization and chunking with overlap; persisted `CourseDocument` and ordered `DocumentChunk` records; PDF page metadata.
- `mvnd clean test`: PASS, 14 tests, 0 failures, 0 errors, 0 skipped.
- `mvnd clean package`: PASS; executable JAR created at `target/studyforge-ai-0.1.0-SNAPSHOT.jar`.
- Web verification: materials page returned HTTP 200; TXT, Markdown, and Stage 1 PDF uploads processed; material metadata/list/detail rendered; duplicate and unsupported-file paths returned controlled feedback.
- CLI verification: `material list`, `material add`, and `material show` use course codes and shared the same persisted data; duplicate material returned exit code 1 with a concise error and no stack trace.
- Persistence verification: document records remained available after stopping and restarting the packaged application.
- Phase 1 course routes and the existing 8 tests remained passing.
- Known limitations: OCR is not implemented; image-only/scanned PDFs with no extractable text are rejected; retrieval and AI features are not implemented.
- Phase 3 features remain not started.

## Phase 3

Status: IMPLEMENTED + VERIFIED

- F03 deterministic course-material search implemented over persisted `DocumentChunk` records.
- Retrieval is scoped to one course and `READY` documents only; failed/non-ready content is excluded by the repository query.
- Lexical scoring combines exact normalized phrase match (6.0), query-term coverage (4.0 maximum), matched-term contribution (1.5 per term), bounded term frequency (0.25 per capped occurrence), and a modest all-terms filename boost (2.0).
- Default top-K is 5; maximum top-K is 20; minimum score is centralized at 1.0. Equal scores use document ID then chunk index ordering.
- `mvnd test`: PASS, 23 tests, 0 failures, 0 errors, 0 skipped.
- `mvnd clean package`: PASS; executable JAR created at `target/studyforge-ai-0.1.0-SNAPSHOT.jar`.
- Web search verified for strategy/pattern, dependency inversion, and no-result queries; results show source filenames, snippets, chunk indexes, and PDF pages.
- CLI `material search` verified against the same persisted data and retrieval service; no-result and invalid-query behavior are controlled.
- Search persisted across application restart and continued to return existing material chunks.
- No Ollama, PaddleOCR, embeddings, vector database, or grounded Q&A was added.
- The Phase 4 AI foundation was intentionally outside the Phase 3 implementation scope.

## Phase 4 — AI foundation and Ollama adapter

Status: **IMPLEMENTED + VERIFIED**

- Added provider-neutral `LlmClient`, typed request/response and availability contracts.
- Added `OllamaLlmAdapter` using Java 21 `HttpClient`, Jackson, `/api/tags`, and non-streaming `/api/generate`.
- Added configurable provider, base URL, model, enablement, connect timeout, and request timeout.
- Added prompt section builder, controlled structured JSON response parser, and `studyforge ai status`.
- Startup performs no Ollama network check. `mvnd clean test` passed with 29 tests; local Ollama 0.34.1 `/api/tags` returned HTTP 200 and included `qwen2.5-coder:14b`; adapter behavior was verified with a local HTTP test server.

No Q&A, summaries, embeddings, vector database, OCR, or agent orchestration was added. Those remain later scope.

## Phase 5 — F04 grounded course Q&A

Status: **IMPLEMENTED + VERIFIED**

- Added `GroundedQuestionService`, shared by the web and CLI interfaces.
- Reused F03 course-scoped retrieval before any model call; empty retrieval short-circuits without calling the LLM.
- Added deterministic `S1`, `S2`, ... source mapping, bounded grounding context, PDF page preservation, and source-reference validation.
- Added strict grounding/prompt-injection instructions, structured response parsing, model-reported insufficiency handling, and controlled AI-unavailable errors.
- Added web route `/courses/{courseId}/ask` and CLI command `studyforge ask --course ... --question ...`.
- `mvnd clean test`: PASS, 34 tests, 0 failures, 0 errors, 0 skipped.
- Packaged JAR starts successfully; `/` and `/courses/1/ask` returned HTTP 200.

No embeddings, vector search, OCR, chat memory, or agent orchestration was added.

## UI usability refinement

Status: **IMPLEMENTED + VERIFIED**

- Replaced the stale disabled Ask StudyForge navigation item with active course-aware navigation and a visible planned-features section.
- Updated the application badge to Phase 5, added course-workspace workflow cards, clearer materials onboarding, Q&A examples/loading feedback, inline recovery actions, AI status page, source presentation improvements, focus states, and responsive mobile navigation.
- Browser verification confirmed dashboard, course workspace, and Q&A pages render successfully; course-specific action links preserve course ID.
- Final automated verification: `mvnd clean test` passed with 38 tests, 0 failures, 0 errors, 0 skipped; packaged JAR started successfully.

## Document-specific Q&A workspace

Status: **IMPLEMENTED + VERIFIED**

- Added document-scoped retrieval and grounding so a selected PDF can be questioned without including other course materials.
- Added safe inline document serving and a split PDF/chat workspace at `/courses/{courseId}/materials/{documentId}`.
- Added document-only suggested questions, answer/source display, loading feedback, and recovery messaging.
- Browser verification confirmed the PDF preview and `Ask this PDF` panel render together for `EECS3311-5-SOLID.pdf`.
- Final automated verification: `mvnd test` passed with 40 tests, 0 failures, 0 errors, 0 skipped; packaged JAR started successfully.

## Phase 6 — F05 summarization and F06 concept extraction

Status: **IMPLEMENTED + VERIFIED**

- F05 document summaries reuse persisted, deterministically ordered chunks and a bounded 12,000-character grounding context. Topic summaries reuse F03 course-scoped lexical retrieval and skip the LLM when no evidence is found.
- F06 extracts structured concepts from a selected READY document, validates every model source ID, and merges duplicate names using Unicode normalization, trimming, whitespace normalization, and case folding while preserving first-seen order and merging sources.
- F05/F06 depend on the provider-neutral `LlmClient`; neither application service depends on Ollama. AI responses, summaries, concepts, and snippets are rendered with escaped Thymeleaf output.
- Web routes: `/courses/{courseId}/materials/{documentId}/summary`, `/courses/{courseId}/summary/topic`, `/courses/{courseId}/materials/{documentId}/concepts`, and `/courses/{courseId}/concepts`.
- CLI routes: `summary document`, `summary topic`, and `concepts extract`, all sharing the same facade/services as the web UI.
- Results are request-based; the current schema has no summary/concept persistence tables, so the Stage 1 “may be recorded” behavior is deferred until a later persistence phase.
- `mvnd clean test`: PASS, 40 tests, 0 failures, 0 errors, 0 skipped; `mvnd clean package`: PASS; packaged JAR starts on port 8080 with persisted course/document data.
- Real-provider verification: `/ai/status` reported Ollama AVAILABLE with the configured provider/model; document summary and concept extraction POSTs for course 1/document 6 returned HTTP 200 and rendered their result pages.
- F07 Flashcards and all later features remain not started.

## Phase 7 — F07 flashcard generation, persistence, and review

Status: **IMPLEMENTED; deterministic and packaged verification complete; real-provider generation remains model-output dependent**

- Added persisted `Flashcard`, `FlashcardSource`, `FlashcardReview`, and `FlashcardReviewRating` models with course ownership, document/chunk/page provenance, append-only reviews, and deterministic ID ordering.
- Added provider-neutral `FlashcardGenerationService` with READY-document validation, bounded 12,000-character context, structured output, strict source-ID validation, field validation, and deterministic question deduplication.
- Added deterministic `FlashcardReviewService`; reviewing existing cards does not call the LLM and supports INCORRECT, HARD, GOOD, and EASY ratings.
- Added web routes `/courses/{courseId}/flashcards`, `/courses/{courseId}/flashcards/generate`, `/courses/{courseId}/flashcards/review`, and `/courses/{courseId}/flashcards/{cardId}/review`.
- Added CLI commands `flashcards generate`, `flashcards list`, and `flashcards review`.
- Flashcards, Quizzes, Study Plan, Progress, Next step, History, and Agent assistance are active course-scoped tools.
- `mvnd clean test`: PASS, 42 tests, 0 failures, 0 errors, 0 skipped; `mvnd package`: PASS; packaged JAR starts and preserves the existing course/material database.
- Real-provider attempts against both locally available Ollama models returned controlled “AI flashcards could not be processed safely” responses; no invalid cards were persisted.
- F08-F14 and the bounded agent orchestration layer are implemented in the current working tree. See `docs/final-implementation-status.md` for the current verification checkpoint and known limitations.

## Current master-audit checkpoint

The historical phase notes above are preserved for traceability. The current source state is packaged and verified through F01–F14 plus bounded agent orchestration. The latest clean regression is 53 tests with 0 failures/errors. Runtime checks cover restart persistence, course isolation, CLI help/course/AI-status smoke flows, AI-disabled startup, the complete primary web learning flow, quiz submission, and deterministic agent behavioral cases. Final KUMA execution is not claimed because no KUMA executable or repository is available in the workspace.
