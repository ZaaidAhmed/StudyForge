# StudyForge implemented architecture

StudyForge is a local-first Spring Boot modular monolith. The diagram below describes the architecture that is implemented in the source repository, not a future design.

```mermaid
flowchart TB
    Student([Student])
    Browser[Browser GUI<br/>Spring MVC + Thymeleaf]
    CLI[Picocli CLI]

    subgraph App[StudyForge application]
        Web[Web controllers]
        Commands[CLI commands]
        Facade[StudyAgentFacade]
        Services[Application services]
        Agent[AgentCoordinator<br/>bounded plan and whitelisted tools]
        Retrieval[CourseSearchService<br/>deterministic lexical retrieval]
        Context[GroundingContextBuilder<br/>bounded context and source IDs]
        AI[LlmClient<br/>PromptBuilder + StructuredResponseParser]
        Repositories[Spring Data repositories]
    end

    Uploads[(data/uploads<br/>PDF, TXT, MD)]
    H2[(H2 file database<br/>courses, chunks, quizzes,<br/>cards, reviews, plans)]
    Ollama[Ollama local HTTP API<br/>qwen3:4b]

    Student --> Browser --> Web --> Facade
    Student --> CLI --> Commands --> Facade
    Facade --> Services
    Facade --> Agent
    Agent --> Services
    Services --> Retrieval --> H2
    Services --> Context --> AI --> Ollama
    Services --> Repositories --> H2
    Services --> Uploads

    classDef entry fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
    classDef core fill:#f5f3ff,stroke:#7c3aed,color:#2e1065
    classDef data fill:#ecfdf5,stroke:#059669,color:#064e3b
    classDef external fill:#fff7ed,stroke:#ea580c,color:#7c2d12
    class Student,Browser,CLI,Web,Commands entry
    class Facade,Services,Agent,Retrieval,Context,AI,Repositories core
    class Uploads,H2 data
    class Ollama external
```

## Evidence and AI flow

```mermaid
flowchart LR
    File[Uploaded document] --> Validate[Validate type, size,<br/>content, duplicate, filename]
    Validate --> Extract[PDF/text extraction]
    Extract --> Chunk[Normalize and chunk<br/>with page metadata]
    Chunk --> Store[(H2 document + chunks)]
    Store --> Search[Course/document-scoped<br/>lexical search]
    Search --> Bound[Bound relevant chunks]
    Bound --> Prompt[Source-only prompt]
    Prompt --> Model[Ollama model]
    Model --> Parse[Parse JSON and validate<br/>fields and source IDs]
    Parse --> Result[Grounded answer or<br/>validated learning content]
```

Model output is untrusted. It is not displayed or persisted until structured parsing and StudyForge-owned source validation succeed. AI failures are recoverable; deterministic search and other local workflows remain usable when Ollama is unavailable.

## Implemented package map

| Layer | Actual package | Responsibility |
|---|---|---|
| Web | `ca.yorku.eecs3311.coursepilot.web` | GUI routes, form handling, recovery states |
| CLI | `ca.yorku.eecs3311.coursepilot.cli` | Picocli commands and exit codes |
| Facade | `ca.yorku.eecs3311.coursepilot.application` | Shared entry point for GUI and CLI |
| Services | `ca.yorku.eecs3311.coursepilot.application.service` | Course, material, search, AI and learning workflows |
| Agent | `ca.yorku.eecs3311.coursepilot.application.agent` | Intent analysis, bounded plans, tools and memory |
| AI | `ca.yorku.eecs3311.coursepilot.application.ai` | Requests, prompts, response contracts and parsing |
| Infrastructure | `ca.yorku.eecs3311.coursepilot.infrastructure` | Ollama and document storage/extraction |
| Domain | `ca.yorku.eecs3311.coursepilot.domain` | Entities, value rules and repositories |

## Deliberate limitations

The current implementation does not use embeddings, a vector database, OCR, authentication, cloud AI, microservices, or arbitrary agent shell/filesystem/HTTP access. Ollama is local and optional; the default model is configurable with `coursepilot.ai.model`.
