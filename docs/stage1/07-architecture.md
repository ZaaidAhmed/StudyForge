# Stage 1 - Architecture

## Layers

- Presentation: Spring MVC controllers, CLI commands, Thymeleaf views, and view models.
- Application: use-case services and DTO validation; owns transactions and coordinates domain operations.
- Agent: intent analysis, planning, plan validation, execution, tools, commands, and context.
- Domain: entities, value objects, enums, policies, events, and repository contracts.
- Persistence: JPA entities/repositories and H2 file-mode configuration.
- Infrastructure: PDFBox extraction, file storage, deterministic retrieval, logging, and external adapters.
- LLM integration: `LlmClient`, Ollama adapter, prompt builder, structured response parsers, and provider configuration.

## Data flow

Material enters through a validated application request, is stored safely, extracted, normalized, chunked, and persisted. Agent requests pass through the facade and coordinator. Tools retrieve deterministic state; only bounded, relevant context is sent to the LLM. Results are validated before becoming domain data, and observable outcomes are recorded in history.

## GUI and CLI

Both interfaces call the same application services and `StudyAgentFacade`; no business rules are duplicated in controllers or commands. This gives the demonstration two entry points with one behavior.

## Testability and future changes

Interfaces at persistence, file extraction, retrieval, LLM, and command boundaries permit unit tests with test doubles. Strategy and Adapter isolate policies and providers. The modular monolith keeps deployment simple while leaving provider, database, and UI changes localized.

## Security and reliability boundaries

Uploads use generated internal names and configured storage roots. Plans use a whitelist and course-scope checks. Model output is untrusted data. AI outage does not prevent deterministic course, material, grading, progress, or history operations.
