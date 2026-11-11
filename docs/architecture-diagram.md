# StudyForge AI Architecture

This document describes the architecture implemented in the current StudyForge source. StudyForge is a Spring Boot modular monolith: the web application and Picocli CLI share application services, domain models, persistence, and provider integrations. The diagrams show the main runtime paths; optional AI calls are separated from deterministic workflows.

## 1. System architecture

```mermaid
flowchart TB
    Student[Student]
    Browser[Browser]
    Terminal[Terminal]

    subgraph App[StudyForge Spring Boot application]
        Web[Spring MVC controllers and Thymeleaf views]
        Cli[Picocli commands]
        Facade[StudyAgentFacade]
        Services[Course and learning services]
        Agent[Bounded agent orchestration]
        Domain[Domain models]
        Repositories[Spring Data JPA repositories]
        AiServices[AI-backed application services]
        AiPort[LlmClient interface]
        Router[RoutingLlmClient]
        OllamaAdapter[OllamaLlmAdapter]
        GeminiAdapter[GeminiLlmAdapter]
        UnsupportedAdapter[UnavailableLlmClient]
        Properties[AiProperties]
    end

    Uploads[(Local uploaded files)]
    H2[(File-backed H2 database)]
    Ollama[(Ollama local API)]
    Gemini[(Gemini API)]

    Student --> Browser --> Web
    Student --> Terminal --> Cli
    Web --> Facade
    Cli --> Facade
    Cli -. "some commands inject services directly" .-> Services
    Facade --> Services
    Facade --> Agent
    Agent --> Services
    Services --> Domain
    Domain --> Repositories --> H2
    Services --> Uploads
    AiServices --> AiPort --> Router
    Router --> Properties
    Router --> OllamaAdapter --> Ollama
    Router --> GeminiAdapter --> Gemini
    Router -. "unsupported provider configuration" .-> UnsupportedAdapter

    classDef entry fill:#e8f1ff,stroke:#2563a6,color:#102a43
    classDef app fill:#eaf7ef,stroke:#2f855a,color:#173b2b
    classDef ai fill:#f3e8ff,stroke:#7c3aed,color:#312e81
    classDef store fill:#fff4d6,stroke:#b7791f,color:#4a3200
    classDef external fill:#f3f4f6,stroke:#6b7280,color:#1f2937
    class Student,Browser,Terminal entry
    class Web,Cli,Facade,Services,Agent,Domain,Repositories app
    class AiServices,AiPort,Router,OllamaAdapter,GeminiAdapter,UnsupportedAdapter,Properties ai
    class Uploads,H2 store
    class Ollama,Gemini external
```

The application keeps presentation, use-case orchestration, provider adapters, and persistence in one deployable process. Web controllers and CLI commands translate user input to shared application behavior; a small number of CLI commands use application services directly.

## 2. Request and persistence flow

```mermaid
flowchart LR
    Request[Course-scoped user request]
    Choice{Web or CLI}
    Controller[Spring MVC controller]
    Command[Picocli command]
    Facade[StudyAgentFacade]
    Service[Application service]
    Model[Domain model]
    Repository[Course-scoped repository]
    Database[(H2 database)]
    View[Thymeleaf response]
    Output[CLI output]

    Request --> Choice
    Choice --> Controller --> Facade
    Choice --> Command --> Facade
    Controller -. "direct service injection for selected workflows" .-> Service
    Command -. "direct service injection for selected commands" .-> Service
    Facade --> Service --> Model --> Repository --> Database
    Database --> Repository --> Service
    Service --> View
    Service --> Output

    classDef user fill:#e8f1ff,stroke:#2563a6,color:#102a43
    classDef logic fill:#eaf7ef,stroke:#2f855a,color:#173b2b
    classDef data fill:#fff4d6,stroke:#b7791f,color:#4a3200
    class Request,Choice,Controller,Command,View,Output user
    class Facade,Service,Model logic
    class Repository,Database data
```

Course, document, quiz, flashcard, attempt, review, and study-plan operations are validated in application services and queried through course-scoped repositories. The database is stored at `./data/coursepilot`; uploaded source files are stored separately under `./data/uploads`.

## 3. Material, retrieval, and AI flow

```mermaid
flowchart TB
    Upload[PDF, TXT, or Markdown upload]
    Validate[Validate type, size, and filename]
    Store[Store under generated safe filename]
    Extract[Extract text]
    Normalize[Normalize text]
    Chunk[Create overlapping chunks with page metadata]
    Persist[Persist document and chunks]
    Search[CourseSearchService lexical ranking]
    Ground[Build bounded evidence and source IDs]
    Prompt[PromptBuilder creates task prompt]
    Port[LlmClient]
    Route[RoutingLlmClient]
    Provider{Selected provider}
    Ollama[Ollama adapter and local model]
    Gemini[Gemini adapter and hosted model]
    Parse[Parse structured response]
    ValidateOutput[Validate schema, source ownership, and content]
    Result[Answer, summary, concepts, quiz, or flashcards]
    NoEvidence[Controlled insufficient-evidence result]

    Upload --> Validate --> Store --> Extract --> Normalize --> Chunk --> Persist
    Persist --> Search --> Ground --> Prompt --> Port --> Route --> Provider
    Provider --> Ollama --> Parse
    Provider --> Gemini --> Parse
    Parse --> ValidateOutput --> Result
    Search -. "no usable evidence" .-> NoEvidence

    classDef source fill:#e8f1ff,stroke:#2563a6,color:#102a43
    classDef process fill:#eaf7ef,stroke:#2f855a,color:#173b2b
    classDef ai fill:#f3e8ff,stroke:#7c3aed,color:#312e81
    classDef outcome fill:#fff4d6,stroke:#b7791f,color:#4a3200
    class Upload,Ollama,Gemini source
    class Validate,Store,Extract,Normalize,Chunk,Persist,Search,Ground process
    class Prompt,Port,Route,Provider,Parse,ValidateOutput ai
    class Result,NoEvidence outcome
```

Retrieval is deterministic lexical search over the selected course's ready document chunks. AI-backed features receive bounded evidence; parsed outputs and cited source IDs are validated before results are displayed or saved. If AI is unavailable, deterministic workflows such as search, objective quiz grading, progress aggregation, study planning, and history remain available.

## 4. Bounded agent, history, and planning

```mermaid
flowchart LR
    UserRequest[Course-scoped agent request]
    Intent[IntentAnalyzer]
    Planner[TaskPlanner]
    Validator[AgentPlanValidator]
    ToolManager[AgentToolManager allowlist]
    Services[Approved course services]
    Synthesis[ResponseSynthesizer]
    Answer[Student-facing response]
    History[LearningHistoryService]
    Memory[MemoryManager latest 10 events]
    Events[Persisted reviews, quiz attempts, and plans]
    Progress[ProgressService]
    Recommendation[RecommendationService]
    StudyPlan[StudyPlanService and strategies]

    UserRequest --> Intent --> Planner --> Validator --> ToolManager --> Services --> Synthesis --> Answer
    Events --> History --> Memory --> Synthesis
    Services --> Progress --> Recommendation
    Services --> StudyPlan

    classDef input fill:#e8f1ff,stroke:#2563a6,color:#102a43
    classDef control fill:#f3e8ff,stroke:#7c3aed,color:#312e81
    classDef service fill:#eaf7ef,stroke:#2f855a,color:#173b2b
    classDef data fill:#fff4d6,stroke:#b7791f,color:#4a3200
    class UserRequest,Answer input
    class Intent,Planner,Validator,ToolManager,Synthesis,Memory control
    class Services,Progress,Recommendation,StudyPlan,History service
    class Events data
```

The agent validates proposed plans before executing allowlisted tools and uses at most ten recent persisted learning events when synthesizing relevant history or progress responses. The history feature is derived from flashcard reviews, quiz attempts, and study-plan creation. Study plans use deterministic strategies (balanced, weak-topic focused, or exam focused) to distribute the submitted time budget across dated activities.

## Architecture boundaries

- The web app and CLI are interfaces to the same Spring Boot application; this is not a microservice deployment.
- Ollama and Gemini are optional provider integrations selected through `AiProperties` and `RoutingLlmClient`.
- User uploads and the H2 database are local runtime data, separate from source code.
- Authentication, multi-user identity, OCR, embeddings/vector search, and cloud deployment are not implemented.
