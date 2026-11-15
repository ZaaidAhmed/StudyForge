# Stage 2 AI Collaboration and Usage Record

I used AI both as development assistance and as an optional runtime capability in StudyForge. These are separate uses and their figures are not combined. This record identifies the tools, models, work areas, my review, and the available usage and cost evidence. Where I do not have telemetry, I label retrospective estimates clearly and distinguish them from measured account usage.

## AI-assisted development

| Tool/platform and model | Work and representative tasks | My review and contribution | Total token usage | Money cost |
|---|---|---|---|---|
| OpenAI Codex coding assistant; GPT-5 for Phases 1-3 and GPT-5.6 Luna for later development work | I used Codex for the project foundation and F01-F15 work, including Spring MVC/Thymeleaf and Picocli workflows, course/material services, grounded retrieval, structured generation and validation, quiz grading, progress, plans, bounded agent/history behavior, provider selection, tests, debugging, and documentation. Representative tasks included diagnosing malformed quiz responses, improving assistant follow-up behavior, and refining saved study-plan and history interfaces. | I set feature scope and acceptance criteria, supplied observed failures, screenshots, and terminal output, reviewed generated changes, requested corrections, and directed integration and verification. I used test/build results and GUI/CLI behavior to review the work; the report records the checks and their tested revision. | **Approximately 1.5 million tokens** combined input and output, as a rough retrospective estimate from the volume of project interactions; this is not provider telemetry and may differ substantially from actual usage. | **Project-attributable total unavailable.** I report an approximate subscription price of **CAD $28 per month** for the account used for development assistance. This is a shared subscription, not a project-specific charge. |

## AI providers used by the application

Runtime inference is product usage, not development-assistant usage. The application does not retain cumulative token or cost totals.

| Provider/platform and model evidence | Application work | Token usage | Money cost |
|---|---|---|---|
| Ollama local runtime; supported models include `qwen3:4b` and `qwen-claude-14b:latest` | I integrated local inference behind the provider-neutral `LlmClient` for supported grounded-answer and generation workflows. The model is selected through application configuration. | **Unavailable.** The application does not record cumulative token usage. | **Hosted API charge: $0** for local inference. Electricity and hardware costs were not measured. |
| Gemini hosted runtime; supported configuration includes `gemini-3.5-flash-lite`, `gemini-3.8-flash`, and `gemini-2.5-flash` | I integrated Gemini through the same provider-neutral client and routing layer. Which model was used for each historical request was not retained. | **Unavailable.** No authoritative project-level usage export was retained, and the application does not record cumulative token usage. | **Project-attributable total unavailable.** No authoritative project-level billing export was retained. |

I keep provider credentials in local configuration and exclude them from this record. The standalone application-provider model list describes supported configuration; it does not claim that every listed model was used during development or for every runtime request.
