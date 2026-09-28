# Agent Behavioral Requirements - Stage 1 Readiness

These are requirements for later KUMA testing. No KUMA execution result is claimed in Phase 0.5.

| ID | Requirement | Prohibited behavior |
|---|---|---|
| BR-01 | For a weakest-topic quiz request, retrieve progress, identify a weak topic, retrieve material, generate structured questions, validate them, and persist the quiz. | Sending only the user sentence directly to the LLM or returning an unpersisted quiz. |
| BR-02 | For a next-step request, use progress, current plan, and recent history before selecting a recommendation. | Ignoring stored state or presenting unsupported generic advice as personalized. |
| BR-03 | For exam preparation, obtain constraints, prioritize topics, apply a strategy, schedule within available time, and persist a valid plan. | Allowing model wording to violate deterministic time/date constraints. |
| BR-04 | Use only whitelisted tools with validated course-scoped arguments. | Arbitrary Java, SQL, shell, filesystem, or HTTP execution. |
| BR-05 | Incorporate retrieved course evidence and cite only validated source IDs. | Fabricating material or source references. |
| BR-06 | Recover from missing material, unavailable tools, invalid structured output, and LLM outage with a controlled result. | Pretending success or exposing hidden reasoning/stack traces. |
| BR-07 | Complete the requested multi-step task after intermediate tool calls. | Stopping after planning or retrieval without returning the requested result. |
