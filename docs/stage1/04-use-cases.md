# Stage 1 - Detailed Use-Case Descriptions

All use cases are initiated by Student. The configured LLM Service is a supporting actor for AI-assisted cases.

## UC01 - Manage Course
- Actors: Student.
- Goal: organize learning work under a course.
- Preconditions: application is running.
- Trigger: student submits a course form or CLI command.
- Main Success Scenario: validate course data, create/update the course, and display its dashboard.
- Alternative / Exception Flows: blank fields or duplicate code return validation errors; missing course returns a friendly not-found result.
- Postconditions: course data is persisted.
- Related Features: F01.

## UC02 - Import Course Material
- Actors: Student.
- Goal: add safe, searchable course material.
- Preconditions: target course exists and file is within configured limits.
- Trigger: student submits an upload or import command.
- Main Success Scenario: validate type/size, hash and deduplicate, safely store, extract, normalize, chunk, and persist source metadata.
- Alternative / Exception Flows: unsupported, oversized, empty, corrupt, unreadable, or duplicate files receive controlled errors; failed imports are not marked complete.
- Postconditions: completed document and chunks are available for retrieval.
- Related Features: F02.

## UC03 - Search Course Material
- Actors: Student, Agent.
- Goal: find relevant course evidence.
- Preconditions: course exists; query is nonblank.
- Trigger: student searches or an agent invokes its search tool.
- Main Success Scenario: tokenize the query, score course-scoped chunks, rank results, and show source/page metadata.
- Alternative / Exception Flows: blank query is rejected; no matches returns guidance.
- Postconditions: no state change beyond optional search history.
- Related Features: F03.

## UC04 - Ask Grounded Course Question
- Actors: Student, LLM Service.
- Goal: receive an answer grounded in uploaded material.
- Preconditions: selected course exists; retrieval and configured LLM are available.
- Trigger: student submits a question.
- Main Success Scenario: analyze intent, plan retrieval, search, validate relevance, build bounded prompt, call LLM, validate source IDs, store session, and display answer with sources.
- Alternative / Exception Flows: insufficient material returns an insufficiency message; LLM outage or invalid output returns a safe error.
- Postconditions: answer and observable session history are available.
- Related Features: F04.

## UC05 - Summarize Course Material
- Actors: Student, LLM Service.
- Goal: understand a document or topic quickly.
- Preconditions: selected source material exists.
- Trigger: student requests a summary.
- Main Success Scenario: retrieve bounded sections, summarize sections if needed, combine structured sections, validate sources, and display the result.
- Alternative / Exception Flows: missing content, oversized input, or AI outage is reported without fabricated content.
- Postconditions: summary is displayed and may be recorded in history.
- Related Features: F05.

## UC06 - Extract Concepts
- Actors: Student, Agent, LLM Service.
- Goal: create a useful concept list from course material.
- Preconditions: selected course material exists.
- Trigger: student or agent requests extraction.
- Main Success Scenario: retrieve context, request structured concepts, validate names/source IDs, deduplicate case-insensitively, persist, and display concepts.
- Alternative / Exception Flows: invalid JSON or unsupported source references cause controlled failure.
- Postconditions: concepts are persisted with supporting sources.
- Related Features: F06.

## UC07 - Generate and Review Flashcards
- Actors: Student, LLM Service.
- Goal: study with sourced flashcards and record performance.
- Preconditions: course material exists for generation.
- Trigger: student requests generation or submits a review rating.
- Main Success Scenario: retrieve context, generate and validate cards, persist them, reveal cards, and record Incorrect/Hard/Good/Easy attempts.
- Alternative / Exception Flows: invalid card output is rejected; missing card or rating is reported.
- Postconditions: flashcards and review history are persisted.
- Related Features: F07.

## UC08 - Generate Quiz
- Actors: Student, Agent, LLM Service.
- Goal: create a valid quiz for a course or weak topic.
- Preconditions: course exists; relevant material or concepts are available.
- Trigger: student requests a quiz or agent receives weakest-topic quiz request.
- Main Success Scenario: load progress when needed, identify topic, retrieve evidence, generate structured questions, validate source IDs and constraints, persist quiz, and return it.
- Alternative / Exception Flows: no weak topic/material returns a controlled result; invalid output is retried once then fails safely.
- Postconditions: quiz and questions are persisted.
- Related Features: F08.

## UC09 - Take and Grade Quiz
- Actors: Student.
- Goal: complete a quiz and receive deterministic results.
- Preconditions: quiz exists and belongs to the selected course.
- Trigger: student submits answers.
- Main Success Scenario: validate attempt, grade objective questions, record `QuestionResponse` values, calculate score, publish completion event, and display results.
- Alternative / Exception Flows: missing/duplicate answers or invalid quiz state is rejected; short-answer feedback is separately labeled AI-assisted.
- Postconditions: quiz attempt is persisted and progress update is triggered.
- Related Features: F09.

## UC10 - Explain Incorrect Answer
- Actors: Student, LLM Service.
- Goal: understand an incorrect response.
- Preconditions: question and response exist; supporting course material may be available.
- Trigger: student selects Explain after a result.
- Main Success Scenario: retrieve evidence, build a prompt with question/response/rubric, call LLM, validate sources, label feedback as AI-assisted, and display it.
- Alternative / Exception Flows: insufficient evidence or LLM outage returns a controlled explanation-unavailable result.
- Postconditions: feedback may be recorded without storing hidden reasoning.
- Related Features: F10.

## UC11 - View Progress / Weak Topics
- Actors: Student, Agent.
- Goal: understand mastery and weak areas.
- Preconditions: course exists.
- Trigger: student opens progress or an agent invokes progress tool.
- Main Success Scenario: aggregate attempts, calculate topic metrics, distinguish no-data from low score, rank weak topics, and display results.
- Alternative / Exception Flows: no attempts displays an empty-state explanation.
- Postconditions: progress view is available.
- Related Features: F11.

## UC12 - Generate Study Plan
- Actors: Student, Agent, LLM Service.
- Goal: schedule effective study within constraints.
- Preconditions: course exists; exam date/time horizon and available minutes are valid.
- Trigger: student submits plan constraints or asks the agent to prepare for an exam.
- Main Success Scenario: obtain constraints, load topics/progress/history, select a strategy, schedule `StudyActivity` items deterministically, optionally ask LLM only for descriptions, validate time/date limits, persist, and display plan.
- Alternative / Exception Flows: impossible constraints explain the conflict; invalid AI wording does not override schedule constraints.
- Postconditions: valid plan is persisted.
- Related Features: F12.

## UC13 - Get Adaptive Recommendation
- Actors: Student, Agent.
- Goal: choose the next useful learning action.
- Preconditions: course exists; progress/history may be empty.
- Trigger: student asks what to study next.
- Main Success Scenario: retrieve progress, current plan, recent history, identify relevant weak areas, select a recommendation, and display an explanation.
- Alternative / Exception Flows: no history returns a starter recommendation; unavailable sources are disclosed.
- Postconditions: recommendation is displayed and may be recorded.
- Related Features: F13.

## UC14 - View Learning History
- Actors: Student, Agent.
- Goal: inspect observable prior learning sessions.
- Preconditions: course exists.
- Trigger: student opens history or an agent invokes history tool.
- Main Success Scenario: retrieve bounded session records, filter by course/topic, and display requests, tools, outcomes, and sources.
- Alternative / Exception Flows: no history returns an empty state; storage failure is handled without corrupting the primary operation.
- Postconditions: history remains unchanged.
- Related Features: F14.
