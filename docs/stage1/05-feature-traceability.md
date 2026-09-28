# Stage 1 - Feature-to-Design Traceability

| Feature | Description | Type | Related Use Case | Classes | Key Methods | Sequence Diagram | Design Pattern(s) |
|---|---|---|---|---|---|---|---|
| F01 | Manage courses | Deterministic | UC01 | `CourseController`, `CourseService`, `CourseRepository` | `createCourse`, `updateCourse`, `findByCode` | None required; simple CRUD is described by UC01 | MVC, Repository |
| F02 | Import and process PDF/TXT/MD | Deterministic | UC02 | `MaterialController`, `CourseMaterialService`, `DocumentValidator`, `DocumentExtractorFactory`, `PdfDocumentExtractor`, `TextDocumentExtractor`, `TextNormalizer`, `TextChunker`, `FileStorageService`, document repositories | `importMaterial`, `validate`, `extract`, `normalize`, `chunk` | SD01 | MVC, Strategy-like extractor boundary, Repository |
| F03 | Search course material | Deterministic | UC03 | `MaterialController`, `MaterialSearchCommand`, `CourseSearchService`, `QueryNormalizer`, `RetrievalScorer`, `DocumentChunkRepository`, `SearchResult` | `search`, `normalize`, `score` | SD08 | MVC, Repository |
| F04 | Grounded course Q&A | Hybrid | UC04 | `StudyAgentFacade`, `AgentCoordinator`, `QuestionAnsweringService`, `PromptBuilder`, `LlmClient` | `ask`, `coordinate`, `askQuestion` | SD02 | Facade, Command, Adapter |
| F05 | Summarize material | Hybrid | UC05 | `SummarizationService`, `PromptBuilder`, `LlmClient` | `summarize` | SD02 structure | Adapter |
| F06 | Extract concepts | Hybrid | UC06 | `ConceptService`, `ConceptTool`, `StructuredResponseParser` | `extractConcepts`, `parseConcepts` | SD02 structure | Command, Adapter |
| F05 | Phase 6 realization | Hybrid | UC05 | `SummarizationService`, `PromptBuilder`, `LlmClient` | `summarizeDocument`, `summarizeTopic` | `sequence-summarize-material.puml` | Adapter |
| F06 | Phase 6 realization | Hybrid | UC06 | `ConceptExtractionService`, `StructuredResponseParser`, `LlmClient` | `extract` | `sequence-extract-concepts.puml` | Adapter |
| F07 | Flashcard generation and review | Hybrid | UC07 | `FlashcardGenerationService`, `FlashcardReviewService`, `FlashcardRepository`, `FlashcardReviewRepository` | `generate`, `review`, `list` | `sequence-flashcard-generation.puml`, `sequence-flashcard-review.puml` | MVC, Adapter |
| F07 | Generate/review flashcards | Hybrid | UC07 | `FlashcardController`, `FlashcardService`, `MaterialSearchService`, repositories | `generateFlashcards`, `recordReview` | SD07 | Adapter, Repository |
| F08 | Generate quizzes | Hybrid | UC08 | `QuizController`, `QuizGenerationTool`, `QuizService`, `StructuredResponseParser` | `generateQuiz`, `parseQuiz` | SD03 | Facade, Command, Adapter |
| F09 | Take/grade quizzes | Deterministic plus isolated AI feedback | UC09 | `QuizController`, `QuizService`, `GradingService`, `QuizAttemptRepository` | `submitQuiz`, `grade` | SD04 | Observer, Repository |
| F10 | Explain wrong answers | Hybrid | UC10 | `QuestionAnsweringService`, `PromptBuilder`, `LlmClient` | `explainMistake` | SD02 structure | Adapter |
| F11 | Track progress/weak topics | Deterministic | UC11 | `ProgressService`, `ProgressUpdateListener`, `TopicProgressRepository` | `updateProgress`, `findWeakTopics` | SD04, SD06 | Observer, Repository |
| F12 | Generate study plans | Hybrid | UC12 | `StudyPlanService`, `WeakTopicAnalyzer`, `StudyStrategy`, `Scheduler` | `createPlan`, `allocate`, `schedule` | SD05 | Strategy, Facade |
| F13 | Recommend next step | Hybrid | UC13 | `RecommendationService`, `ProgressTool`, `StudyPlanTool`, `HistoryTool` | `recommendNextStep` | SD06 | Command, Facade |
| F14 | Learning history/memory | Deterministic | UC14 | `LearningHistoryService`, `HistoryTool`, `LearningSessionRepository` | `recordSession`, `findRelevantHistory` | SD02, SD06 | Command, Repository |

The shared diagrams are intentional: SD02 represents the common retrieve-ground-prompt-LLM path used by F04-F06/F10, while SD04 represents the event path used by F09 and F11.

## Final implementation reconciliation

The rows above preserve the approved Stage 1 vocabulary. The following mapping is authoritative for the implemented F07–F14 code and replaces any earlier placeholder names such as `QuizService`, `QuestionAnsweringService`, or `LearningSessionRepository`:

| Feature | Implemented realization | Runtime evidence |
|---|---|---|
| F07 | `FlashcardGenerationService`, `FlashcardReviewService`, `FlashcardRepository`, `FlashcardReviewRepository`, `FlashcardSource` | Web/CLI generation and append-only review flow |
| F08 | `QuizGenerationService`, `QuizRepository`, `QuizQuestion`, `QuizSource`, `QuizGenerationLlmResponse` | Grounded quiz generation persists validated questions and sources |
| F09 | `QuizAttemptService`, `QuizAttemptRepository`, `QuizAttempt`, `QuizResponse` | Deterministic MCQ/true-false grading and persisted attempts |
| F10 | `WrongAnswerExplanationService`, `GroundedExplanation`, `PromptBuilder`, `LlmClient` | Course-source validation and controlled AI fallback |
| F11 | `ProgressService`, `ProgressSummary` | Deterministic rating/quiz aggregation and weak-topic classification |
| F12 | `StudyPlanService`, `StudyPlanningStrategy`, `BalancedStudyStrategy`, `WeakTopicFocusedStrategy`, `ExamCramStrategy`, `StudyPlanRepository` | Persisted constrained schedules with distinct strategies |
| F13 | `RecommendationService`, `Recommendation`, `RecommendationAction` | Whitelisted deterministic next-step selection |
| F14 | `LearningHistoryService`, `HistoryEvent`, persisted flashcard reviews/quiz attempts/study plans | Bounded course-scoped chronological history |
| Agent | `StudyAgentFacade`, `AgentCoordinator`, `IntentAnalyzer`, `TaskPlanner`, `AgentPlanValidator`, `AgentToolManager`, `MemoryManager`, `ResponseSynthesizer` | Bounded observable plans and whitelisted existing-service tools |
