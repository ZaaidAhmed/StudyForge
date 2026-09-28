# Phase 8 — Grounded quizzes and quiz attempts

Status: implemented and packaged.

- Course-owned persisted quizzes contain multiple-choice, true/false, and short-answer questions with document/chunk provenance.
- Web routes provide generation, listing, taking, and submitting quizzes while preserving course context.
- MCQ and true/false answers are graded deterministically. Short answers use a bounded reference-answer match and persist feedback.
- CLI commands: `coursepilot quiz generate` and `coursepilot quiz list`.
- Verification: `mvnd clean test` passed with 42 tests; `mvnd package` passed; the packaged application starts on port 8080.
