# StudyForge AI implementation checkpoint

## Implemented feature surface

F01–F06 remain available through the original course, materials, retrieval, grounded Q&A, summary, and concepts services. F07 adds persisted grounded flashcards and append-only review ratings. F08 adds persisted grounded quizzes. F09/F10 add course-owned attempts, deterministic objective grading, provider-neutral short-answer grading, and grounded wrong-answer explanations. F11 computes deterministic progress and weak-topic signals. F12 persists Balanced, Weak Topic Focused, and Exam Cram plans. F13/F14 provide deterministic recommendations and bounded chronological learning history.

## Agent architecture

`StudyAgentFacade` delegates high-level requests to `AgentCoordinator`. The coordinator uses `IntentAnalyzer`, `TaskPlanner`, `AgentPlanValidator`, `AgentToolManager`, `MemoryManager`, and `ResponseSynthesizer`. Plans contain observable steps only, are limited to five steps, and execute only whitelisted course-scoped tools for search, progress, weak topics, history, recommendations, summaries, flashcards, quizzes, and study plans. No shell, filesystem, SQL, arbitrary HTTP, reflection, or chain-of-thought tool exists.

## Verification checkpoint

- `mvnd clean package`: 50 tests passed, 0 failures, 0 errors, 0 skipped; executable JAR packaged successfully.
- `mvnd clean package`: successful.
- Packaged JAR: starts successfully on port 8080.
- Browser smoke checks: quiz, progress, study plan, history, recommendation, and agent pages returned successfully with course-scoped navigation.
- Restart check: persisted course, material, quiz, flashcard, history, and progress pages remained available after stopping and restarting the packaged JAR.
- Course isolation check: a temporary second course returned no first-course material, quiz, flashcard, history, progress, or Q&A evidence; it was removed after verification.
- CLI smoke check: CLI help, course listing, and AI status completed successfully against the persisted database.
- AI-offline check: a second instance with AI disabled kept the landing, course list, and AI status pages available with controlled disabled status.
- AI-offline persisted-course check: with the real course/document database mounted, document Q&A, flashcard generation, and quiz generation returned HTTP 200 pages containing controlled disabled/unavailable guidance; no crash or data corruption occurred.
- Deterministic behavioral checks: agent plan execution, tool-failure surfacing, bounded memory retrieval, grounded source validation, malformed structured output, and prompt-injection framing are covered without requiring Ollama.
- End-to-end runtime scenario: search, document-scoped Q&A, summary, concepts, flashcards, quiz generation, quiz submission, history, progress, study plan, recommendation, and agent progress assistance all returned successful controlled responses against the packaged JAR.
- Agent search parameter flow: a real “Find SOLID in my course materials” request produced a `COURSE_SEARCH` plan and returned course-scoped SOLID material results.
- Agent isolation check: a temporary course received a successful progress-agent response without course 1 document/question content and was removed afterward.

## Known limitations

Real AI generation remains dependent on the configured Ollama model producing valid structured output. When AI is unavailable or malformed, deterministic features remain usable and AI-dependent operations return controlled recovery states. OCR, embeddings, vector search, and cloud deployment are intentionally out of scope. Deterministic behavioral coverage is now present; an external KUMA run has not been claimed because no KUMA executable/repository is present in the workspace. The primary F01–F14 end-to-end scenario and required runtime gates are verified; the external KUMA execution remains the only environment-dependent verification not run.
