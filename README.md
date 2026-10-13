# StudyForge

[![Ollama](https://img.shields.io/badge/Ollama-local%20LLM%20experimentation-000000?style=flat&logo=ollama&logoColor=white)](https://ollama.com/)
[![Course project](https://img.shields.io/badge/Project-EECS%203311-6A1B9A?style=flat)](https://www.eecs.yorku.ca/)
[![Status](https://img.shields.io/badge/Status-implemented%20prototype-green?style=flat)](https://github.com/ZaaidAhmed/StudyForge)

StudyForge is an implemented local-first study-support prototype created for an EECS 3311 course project. It combines a Spring MVC + Thymeleaf GUI, a Picocli CLI, deterministic course/material/search workflows, grounded learning tools, and bounded Ollama-based AI features.

## Current focus

- Using locally hosted language models to support grounded study workflows.
- Keeping retrieval, validation, persistence, and AI provider boundaries explicit.
- Continuing to refine the implemented prototype with automated and manual verification.

## Why this project matters

StudyForge reflects an interest in building practical AI tools with clear boundaries, understandable architecture, and a useful end-user purpose. The design-first workflow helps connect user needs, technical decisions, and future implementation work.

## Repository contents

- [Stage 1 design proposal](docs/StudyForge_Stage_1_Design_Proposal.pdf)
- [Implemented architecture](docs/architecture-diagram.md)
- Spring Boot application source, tests, and UI resources

## Project status

This is an active course project. The repository contains the design proposal, implemented prototype, architecture documentation, and verification artifacts.

## Author

Zaaid Ahmed — [GitHub](https://github.com/ZaaidAhmed) · [Portfolio](https://zaaidahmed.netlify.app/)

## Implemented architecture

The implemented system is a local-first Spring Boot modular monolith. The browser GUI and Picocli CLI share the same application facade and services; they do not implement separate business rules.

```mermaid
flowchart TB
    learner([Learner])
    web[Spring MVC + Thymeleaf]
    cli[Picocli CLI]

    subgraph app[StudyForge application]
        controllers[Web controllers]
        commands[CLI commands]
        facade[StudyAgentFacade]
        services[Application services<br/>courses and materials<br/>search and grounded Q&A<br/>summaries and concepts<br/>flashcards and quizzes<br/>progress, plans, history, recommendations]
        agent[AgentCoordinator<br/>intent -> plan -> validate -> tools]
        evidence[GroundingContextBuilder<br/>bounded chunks and source IDs]
        llm[LlmClient boundary<br/>PromptBuilder + StructuredResponseParser]
        repos[Spring Data repositories]
    end

    files[(Local uploads<br/>PDF, TXT, Markdown)]
    db[(File H2 database)]
    ollama[Ollama HTTP API<br/>qwen3:4b by default]

    learner --> web --> controllers --> facade
    learner --> cli --> commands --> facade
    facade --> services
    facade --> agent
    agent --> services
    services --> evidence
    evidence --> llm
    llm --> ollama
    services --> repos --> db
    services --> files

    classDef entry fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
    classDef core fill:#f5f3ff,stroke:#7c3aed,color:#2e1065
    classDef data fill:#ecfdf5,stroke:#059669,color:#064e3b
    classDef external fill:#fff7ed,stroke:#ea580c,color:#7c2d12
    class learner,web,cli,controllers,commands entry
    class facade,services,agent,evidence,llm,repos core
    class files,db data
    class ollama external
```

### Runtime workflow

1. A course is selected, then a PDF, TXT, or Markdown document is validated and stored with a generated safe filename.
2. Text is extracted, normalized, split into overlapping chunks, and persisted with page/chunk metadata.
3. Deterministic lexical retrieval selects course- or document-scoped evidence.
4. `GroundingContextBuilder` bounds the context and assigns StudyForge-owned source IDs.
5. `PromptBuilder` creates a source-only structured request for the provider-neutral `LlmClient`.
6. `OllamaLlmAdapter` calls the local model. `StructuredResponseParser` validates JSON, required fields, and source references before display or persistence.
7. Learning activity is stored in local H2 and reused by progress, history, recommendations, and bounded agent tools.

### Actual implementation boundaries

- Web entry points: `coursepilot.web` controllers and Thymeleaf templates.
- CLI entry points: `coursepilot.cli` Picocli commands.
- Shared facade: `coursepilot.application.StudyAgentFacade`.
- Business workflows: `coursepilot.application.service`.
- Agent orchestration: `coursepilot.application.agent` with whitelisted course-scoped tools.
- Domain and persistence: `coursepilot.domain.model` and `coursepilot.domain.repository`.
- Document pipeline: `coursepilot.infrastructure.document`.
- AI provider: `coursepilot.infrastructure.ai.OllamaLlmAdapter` behind `LlmClient`.

