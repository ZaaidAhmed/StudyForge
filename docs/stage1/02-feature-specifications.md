# Stage 1 - Feature Specifications

Each feature is specified before implementation. Types are Deterministic, AI, or Hybrid.

## F01 - Course Management

- Description: Create, edit, list, show, and optionally delete courses.
- User interaction: Web forms/dashboard or `studyforge course ...`.
- Input: Course code, name, description, optional exam date.
- Output: Persisted course and dashboard.
- AI involvement: Deterministic.
- Expected workflow: Validate DTO, persist through `CourseService`, return course view.
- Error/alternative: Blank fields and duplicate code receive clear validation errors.

## F02 - Course Material Import and Processing

- Description: Safely import PDF, TXT, and MD files, extract and chunk text.
- User interaction: Upload form or CLI import.
- Input: Course and file within configured size limit.
- Output: Stored document, chunks, processing status, source metadata.
- AI involvement: Deterministic.
- Expected workflow: Validate, hash, deduplicate, safely store, extract, normalize, chunk, persist.
- Error/alternative: Unsupported, empty, corrupt, unreadable, oversized, or duplicate files are controlled failures.

## F03 - Course Material Search / Retrieval

- Description: Search course chunks using transparent deterministic scoring.
- User interaction: Search page or `material search`.
- Input: Course, query, top K.
- Output: Ranked chunks with source and page metadata.
- AI involvement: Deterministic.
- Expected workflow: Tokenize, score, rank, return bounded results.
- Error/alternative: Blank query is rejected; no hits returns an empty result with guidance.

## F04 - Grounded Course Question Answering

- Description: Answer course questions from retrieved material and cite sources.
- User interaction: Ask question page or `ask` command.
- Input: Course and question.
- Output: Answer, source references, learning-session record.
- AI involvement: Hybrid.
- Expected workflow: Analyze intent, retrieve, enforce relevance, build grounded prompt, call LLM, validate sources, store history.
- Error/alternative: Insufficient material and unavailable/invalid AI response never become fabricated answers.

## F05 - Document and Topic Summarization

- Description: Summarize a document, topic, or selected content.
- User interaction: Summary action in web/CLI.
- Input: Course plus document/topic/selection.
- Output: Structured overview, concepts, terms, relationships, review points, and sources.
- AI involvement: Hybrid.
- Expected workflow: Retrieve/bound content, optionally summarize sections, combine validated result.
- Error/alternative: Missing content, oversized input, and AI failure are reported safely.

## F06 - Concept Extraction

- Description: Extract deduplicated concepts with explanations and sources.
- User interaction: Extract concepts action and concept list.
- Input: Course and selected material scope.
- Output: Persisted concepts with source references.
- AI involvement: Hybrid.
- Expected workflow: Retrieve context, request structured JSON, validate and deduplicate, persist.
- Error/alternative: Invalid JSON or unsupported source references produces controlled failure.

## F07 - Flashcard Generation and Review

- Description: Generate sourced flashcards and record review ratings.
- User interaction: Generate/review pages or CLI commands.
- Input: Course and document/topic/concept scope; rating during review.
- Output: Flashcards and `FlashcardAttempt` records.
- AI involvement: Hybrid.
- Expected workflow: Retrieve, generate structured cards, validate, persist, record rating.
- Error/alternative: Invalid card data is rejected; repeated reviews remain historical.

## F08 - Quiz Generation

- Description: Generate a quiz from course material or a weak topic.
- User interaction: Quiz generation form or `quiz generate`.
- Input: Course, topic/scope, count, difficulty.
- Output: Persisted quiz and questions with source references.
- AI involvement: Hybrid.
- Expected workflow: Retrieve context, optionally load weakest topic, generate structured questions, validate, persist.
- Error/alternative: One controlled retry for invalid structured output, then failure.

## F09 - Quiz Taking and Automatic Grading

- Description: Present quizzes, collect answers, and grade deterministic question types.
- User interaction: Quiz-taking page or CLI.
- Input: Quiz answers.
- Output: Score, per-question results, completion event.
- AI involvement: Deterministic for objective grading; AI-assisted only for short answers.
- Expected workflow: Validate ownership, grade objective items, record attempt, publish event.
- Error/alternative: Missing/duplicate answers and invalid quiz state are rejected.

## F10 - Wrong-Answer Explanation

- Description: Explain an incorrect answer using the question, expected answer/rubric, and course evidence.
- User interaction: Explanation action after a quiz.
- Input: Quiz question, answer, and course.
- Output: Explanation with source references.
- AI involvement: Hybrid.
- Expected workflow: Retrieve evidence, call grounded LLM prompt, validate sources, label feedback as AI-assisted.
- Error/alternative: No evidence returns an insufficiency result; no hidden reasoning is stored.

## F11 - Progress Tracking and Weak Topic Analysis

- Description: Aggregate attempts into topic performance and identify weak topics.
- User interaction: Progress dashboard or `progress show`.
- Input: Quiz/flashcard attempts.
- Output: Topic scores, trends, weak-topic ranking.
- AI involvement: Deterministic.
- Expected workflow: Event listener updates progress; deterministic calculations rank topics.
- Error/alternative: New topics have an explicit no-data state, not a misleading zero.

## F12 - Personalized Study Plan Generation

- Description: Generate a plan respecting days, daily minutes, exam date, and learning state.
- User interaction: Plan form or CLI.
- Input: Course, horizon, available time, optional strategy.
- Output: Validated persisted plan and items.
- AI involvement: Hybrid; constraints and scheduling are deterministic.
- Expected workflow: Load progress/history, choose strategy, schedule activities, optionally use LLM for wording, validate constraints, persist.
- Error/alternative: Impossible constraints explain the conflict without pretending success.

## F13 - Adaptive Next-Step Recommendation

- Description: Recommend the next learning action using stored progress and history.
- User interaction: Recommendation page, agent request, or CLI.
- Input: Course and current learning state.
- Output: Recommendation with rationale summary and supporting data.
- AI involvement: Hybrid.
- Expected workflow: Load weak topics, recency, history, and plan; select deterministic candidate; optionally phrase with LLM.
- Error/alternative: No activity history returns a starter recommendation.

## F14 - Learning Session History and Memory

- Description: Persist observable learning sessions and agent outcomes for future adaptation.
- User interaction: History page or CLI.
- Input: User request, selected course, tools used, result summary, sources.
- Output: Searchable history and memory context.
- AI involvement: Deterministic persistence; AI results may be recorded as bounded summaries, never hidden reasoning.
- Expected workflow: Record session after successful/controlled outcome, retrieve relevant prior state for later workflows.
- Error/alternative: History failure is logged and does not corrupt primary quiz/course transactions.
