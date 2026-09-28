# Stage 2 - AI Collaboration Log

This is the canonical implementation log required by the project instructions. The Phase 1 entry is intentionally concise and does not claim user review of individual code decisions.

| Phase | AI tool/platform | Model | Input/instruction | Contribution | Human verification |
|---|---|---|---|---|---|
| Phase 1 | Codex coding agent | GPT-5 | Implement and verify the approved Phase 1 foundation and F01 while preserving the package root and architecture. | Created the Maven/Spring foundation, course domain/service/facade, GUI, CLI foundation, tests, and documentation updates; corrected verification findings in CLI exit handling, test setup, and form validation binding. | PENDING USER REVIEW; automated build, web, CLI, and persistence checks passed. |
| Phase 2 | Codex coding agent | GPT-5 | Implement deterministic F02 document import without starting retrieval or AI phases. | Added JPA document/chunk persistence, validation, SHA-256 duplicate detection, safe storage, PDFBox/TXT/Markdown extraction, normalization, chunking, web materials UI, CLI material commands, tests, and traceability updates. During verification, fixed the CLI wrapped-exception path so duplicate imports produce controlled errors without stack traces. | PENDING USER REVIEW; automated and manual verification completed. |
| Phase 3 | Codex coding agent | GPT-5 | Implement deterministic F03 course-scoped material retrieval without LLM, OCR, embeddings, or vector storage. | Added query normalization, configurable lexical scoring, READY/course-scoped repository retrieval, threshold/top-K ranking, source-aware results, web search, CLI search, tests, and UML/traceability updates. | PENDING USER REVIEW; automated verification completed; packaged web/CLI/persistence demo pending final review. |

Future entries will record representative implementation, debugging, refactoring, and testing interactions. The older `ai-human-collaboration-log.md` remains as the Phase 0.5 template.

## Phase 4

- Implemented the provider-neutral AI foundation and Ollama Adapter with Java `HttpClient` and Jackson.
- Kept Ollama optional at startup; availability is checked only by the explicit status capability.
- Added tests for provider-down behavior, disabled mode, request translation, prompt sections, and fenced JSON parsing.
- Verified the local Ollama installation and configured model without implementing Q&A or later AI features.

## Phase 5

- Implemented F04 as a retrieval-first application service using F03 and the provider-neutral Phase 4 client.
- Added deterministic source IDs, bounded context, strict grounding/prompt-injection instructions, structured response validation, and graceful insufficiency/unavailability behavior.
- Added shared web/CLI flows and fake-client tests covering no-evidence short-circuiting, ordering, source mapping, invalid references, model insufficiency, and prompt framing.

## Phase 6

- Implemented F05/F06 using the existing provider-neutral boundary, persisted chunks, grounding context builder, structured parser, and shared facade.
- Added bounded document/topic summarization, source validation, structured concept extraction, deterministic duplicate merging, web routes/templates, CLI commands, and documentation.
- Automated verification is complete for the existing suite; real-provider and final packaged-JAR/UI verification remain recorded as PENDING USER REVIEW until the rebuilt application is inspected.

## Phase 7

- Added F07 flashcard generation/review, relational provenance and review persistence, structured prompts/responses, active navigation, responsive web pages, and shared CLI commands.
- Existing automated regression remained green. Real Ollama attempts were made with both locally available models; both returned controlled structured-output failures, so no unverified AI cards were persisted. Human review remains PENDING USER REVIEW.
