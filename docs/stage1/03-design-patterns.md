# Stage 1 - Design Patterns

The six guaranteed course patterns are MVC, Facade, Adapter, Strategy, Command, and Observer. Repository remains a supplementary persistence architecture and is not needed to satisfy the pattern count.

## MVC
- Design problem: web request handling, presentation, and learning behavior must not be tangled.
- Participants: Spring MVC controllers, application services/facade, Thymeleaf views/view models.
- Roles: controllers translate HTTP input; services execute use cases; views render results.
- Why appropriate: the project has a substantial GUI and a separate CLI.
- Without it: validation, routing, HTML rendering, and business rules would be duplicated and harder to test.

## Facade
- Design problem: GUI and CLI need the same multi-step agent behavior without exposing orchestration internals.
- Participants: web/CLI boundaries, `StudyAgentFacade`, `AgentCoordinator`, application services.
- Roles: the facade accepts use-case requests; the coordinator plans and executes; services perform domain work.
- Why appropriate: it gives both interfaces one stable application entry point.
- Without it: controllers and commands would duplicate workflow logic and drift apart.

## Adapter
- Design problem: Ollama HTTP details must not leak into application or domain code.
- Participants: `LlmClient`, `OllamaLlmAdapter`, `RestClient`, `PromptBuilder`.
- Roles: `LlmClient` defines the port; the adapter translates typed requests and responses to the Ollama protocol.
- Why appropriate: provider configuration and future provider replacement are isolated.
- Without it: provider-specific JSON, URLs, and outage handling would spread through the application.

## Strategy
- Design problem: study allocation must vary by learning goal without changing plan orchestration.
- Participants: `StudyStrategy`, `BalancedStudyStrategy`, `WeakTopicFocusedStrategy`, `ExamCramStrategy`, `StudyPlanService`.
- Roles: each strategy proposes activities; the service applies hard constraints and persists the plan.
- Why appropriate: the same progress state can support different planning policies.
- Without it: condition-heavy planning code would be difficult to compare, test, and extend.

## Command
- Design problem: agent plans contain multiple actions that must be validated and executed uniformly.
- Participants: `AgentCommand`, concrete commands, `AgentToolManager`, `AgentExecutionEngine`.
- Roles: the manager resolves a fixed tool name; each command executes one allowed action against `AgentContext`.
- Why appropriate: it makes plan execution observable and whitelisted.
- Without it: the coordinator would contain a large conditional dispatcher and model output could be coupled to implementation classes.

## Observer
- Design problem: quiz completion must update progress and adaptation state without coupling quiz-taking to every consumer.
- Participants: `QuizService`, `QuizCompletedEvent`, `ProgressUpdateListener`, `AdaptiveLearningListener`, `ProgressService`.
- Roles: the publisher emits a domain event; listeners react independently.
- Why appropriate: progress and recommendations are downstream consequences of completion.
- Without it: `QuizService` would know and directly invoke every future analytics or adaptation feature.

## Repository (supplementary)

Spring Data repository interfaces isolate database access and make deterministic services easier to test with test doubles. It is documented as an architectural/persistence pattern, not relied upon for the six-pattern course count.
