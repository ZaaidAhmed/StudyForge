# StudyForge Release QA Report

## Current verification (November 7, 2026)

- Runtime: OpenJDK 21.0.12.1; Maven Daemon 1.0.6; Spring Boot MVC, Thymeleaf, JPA/H2, and Picocli.
- Provider boundary supports local Ollama and hosted Gemini. The active provider during this verification was not captured, so no live provider-generation run is claimed.
- `mvnd clean test`: 207 tests, 0 failures, 0 errors, 0 skipped.
- `mvnd package`: passed; package lifecycle reran those tests and created `target/studyforge-ai-0.1.0-SNAPSHOT.jar`.
- Fresh-JAR web smoke: `/`, `/courses`, and `/ai/status` returned HTTP 200 on port 18080.
- Packaged CLI help for `quiz`, `recommend`, and `agent` passed. Read-only `quiz list --course EECS3221`, `recommend --course EECS3221`, and `history --course EECS3221` completed. Quiz generation and agent task execution were not run in this read-only check.
- All 15 YouTube links, 14 release assets, and 15 weekly commit links returned HTTP 200 to unauthenticated HEAD requests. This checks URL response, not playback or video content.
- The regenerated report PDF is 8-page Letter size; the landscape crosswalk and UML overview were visually inspected.
- Remaining submission gates: human video/content and timestamp review; F08/F13 CLI video recording; authoritative AI usage/billing records where available; clean-checkout and instructor-access check.

## Historical verification record (not current release status)

- Runtime: Java 21
- Build: Maven Daemon (`mvnd`)
- Framework: Spring Boot MVC, Thymeleaf, JPA, H2, Picocli
- AI provider: local Ollama through the provider-neutral `LlmClient`
- Latest clean Java 21 test run: 124 tests passing with zero failures and zero errors
- Baseline build verification used OpenJDK 21.0.12.1; the machine default Java 25 was not used by the release verifier.
- Packaged artifact: `target/studyforge-ai-0.1.0-SNAPSHOT.jar`
- Local AI smoke verification: `qwen3:4b` returned valid structured JSON in approximately 0.9 seconds warm-start with Ollama thinking disabled for structured tasks. The previous `qwen-claude-14b:latest` remains available as a quality fallback.
- Live packaged-JAR verification: a POST to `/courses/1/quizzes/generate` with document `6` and count `5` returned HTTP 200 and saved 5 questions after the quiz prompt was tightened with explicit allowed source IDs and JSON field types.
- Quiz reliability verification: generation now requests bounded batches of three, rotates document evidence windows, deduplicates questions, and refuses to persist an incomplete quiz. A request for 20 therefore completes through multiple bounded calls or shows an actionable failure instead of silently saving 8.
- Latest live verification: a 20-question request for `EECS3311-5-SOLID.pdf` returned HTTP 200 and saved exactly 20 questions in approximately 170 seconds. Quiz prompts use rotating document windows; grounded recovery questions fill only model omissions using uploaded-document sentences and source metadata.
- Quiz-taking verification: generated TRUE/FALSE questions now render selectable True and False controls instead of an empty answer area.
- Fresh-JAR smoke verification: passed on temporary port 18080
- Latency smoke: `scripts/measure.ps1` passed against a fresh JAR; three samples each measured `/` at 16.9–19.4 ms, `/courses` at 15.3–18.2 ms, and `/ai/status` at 21.7–62.1 ms on the local environment.
- Verification command: `scripts/verify.ps1` (requires Java 21, builds the JAR, checks core routes, and stops its process)
- Browser DOM/accessibility verification: fresh JAR inspected at `/courses`, `/courses/1`, and `/courses/1/ask`; course context, workflow actions, planned-feature labels, Q&A labels, example questions, and recovery links were present.
- Browser screenshot capture: manual recording remains a submission task; automated DOM/template checks pass.

## Remediation completed

- Corrected the retrieval scorer package path.
- Added CI verification workflow.
- Added proper 404 handling for missing courses and documents.
- Added inline study-plan validation and value preservation.
- Registered quiz, progress, study-plan, recommendation, history, and agent CLI commands.
- Added recoverable validation for missing flashcard, quiz, and agent inputs.
- Added upload MIME-type, size, empty-file, extension, and traversal validation.
- Added quiz count validation, submitted-value preservation, and duplicate-submit feedback.
- Added targeted flashcard and quiz MVC tests.
- Added Picocli command-tree tests for root help, nested help, and required-option failures.
- Added a template scan preventing unescaped `th:utext` output.
- Added deterministic coverage for summaries, concepts, flashcards, quizzes, grading, explanations, progress, study plans, history, and recommendations.
- Added an explicit service-level cross-course document isolation test.
- Added inline duplicate-course recovery with submitted-value preservation.
- Added upload-failure redirect coverage with actionable validation feedback.
- Added behavior-level CLI material-search coverage for success output and missing-course non-zero failure.
- Added retrieval limit-boundary and document-scope isolation coverage.
- Implemented ordered transactional course deletion for dependent learning and material records.
- Added a deterministic successful grounded-summary fixture validating summary text, key points, and source metadata.
- Added a deterministic successful flashcard-generation fixture validating persisted, source-grounded output.
- Added a deterministic successful quiz-generation fixture validating persisted multiple-choice content and source linkage.
- Added a deterministic successful wrong-answer explanation fixture validating grounded evidence metadata.
- Added template contract coverage for READY-document filtering and duplicate-generation protection on flashcards and quizzes.
- Added controlled empty-chunk handling for flashcard and quiz generation.
- Promoted implemented concepts, study plan, progress, recommendations, history, and agent workflows from Planned to active navigation, preserving course context and active-page indicators.
- Added accessible animated AI-request progress feedback with staged retrieval, grounding, and answer-preparation messages plus reduced-motion support.

## Earlier submission evidence list (superseded by the current verification above)

- Full end-to-end tests for F05–F14.
- Public F01-F14 demonstration videos and timestamps.
- Real weekly commit links for each required reporting period.
- Final clean-checkout README walkthrough.
- Repeat local-model latency measurements for the final hardware/model choice.
- Full browser screenshots and keyboard-only evidence for every major page.

## Release recommendation

The current Java 21 test/package and local fresh-JAR smoke gates pass. Submission readiness still depends on the open gates stated in the current verification section.
