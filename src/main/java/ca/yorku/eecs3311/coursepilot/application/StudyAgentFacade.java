package ca.yorku.eecs3311.coursepilot.application;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ImportResult;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedAnswer;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.application.dto.SummaryResult;
import ca.yorku.eecs3311.coursepilot.application.dto.ExtractedConcept;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardGenerationResult;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardReviewResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizGenerationResult;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptResult;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedExplanation;
import ca.yorku.eecs3311.coursepilot.application.service.CourseService;
import ca.yorku.eecs3311.coursepilot.application.service.CourseMaterialService;
import ca.yorku.eecs3311.coursepilot.application.service.CourseSearchService;
import ca.yorku.eecs3311.coursepilot.application.service.GroundedQuestionService;
import ca.yorku.eecs3311.coursepilot.application.service.SummarizationService;
import ca.yorku.eecs3311.coursepilot.application.service.ConceptExtractionService;
import ca.yorku.eecs3311.coursepilot.application.service.FlashcardGenerationService;
import ca.yorku.eecs3311.coursepilot.application.service.FlashcardReviewService;
import ca.yorku.eecs3311.coursepilot.application.service.QuizGenerationService;
import ca.yorku.eecs3311.coursepilot.application.service.QuizAttemptService;
import ca.yorku.eecs3311.coursepilot.application.service.WrongAnswerExplanationService;
import ca.yorku.eecs3311.coursepilot.application.agent.AgentCoordinator;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.core.io.Resource;

@Service
public class StudyAgentFacade {
    private final CourseService courseService;
    private final CourseMaterialService courseMaterialService;
    private final CourseSearchService courseSearchService;
    private final LlmClient llmClient;
    private final GroundedQuestionService groundedQuestionService;
    private final SummarizationService summarizationService;
    private final ConceptExtractionService conceptExtractionService;
    private final FlashcardGenerationService flashcardGenerationService;
    private final FlashcardReviewService flashcardReviewService;
    private final QuizGenerationService quizGenerationService;
    private final QuizAttemptService quizAttemptService;
    private final WrongAnswerExplanationService wrongAnswerExplanationService;
    private final AgentCoordinator agentCoordinator;

    public StudyAgentFacade(CourseService courseService, CourseMaterialService courseMaterialService,
                            CourseSearchService courseSearchService, LlmClient llmClient,
                            GroundedQuestionService groundedQuestionService,
                            SummarizationService summarizationService,
                            ConceptExtractionService conceptExtractionService,
                            FlashcardGenerationService flashcardGenerationService,
                            FlashcardReviewService flashcardReviewService,
                            QuizGenerationService quizGenerationService,
                            QuizAttemptService quizAttemptService,
                            WrongAnswerExplanationService wrongAnswerExplanationService,
                            AgentCoordinator agentCoordinator) {
        this.courseService = courseService;
        this.courseMaterialService = courseMaterialService;
        this.courseSearchService = courseSearchService;
        this.llmClient = llmClient;
        this.groundedQuestionService = groundedQuestionService;
        this.summarizationService = summarizationService;
        this.conceptExtractionService = conceptExtractionService;
        this.flashcardGenerationService = flashcardGenerationService;
        this.flashcardReviewService = flashcardReviewService;
        this.quizGenerationService = quizGenerationService;
        this.quizAttemptService = quizAttemptService;
        this.wrongAnswerExplanationService = wrongAnswerExplanationService;
        this.agentCoordinator = agentCoordinator;
    }

    public CourseResponse createCourse(CreateCourseRequest request) { return courseService.createCourse(request); }
    public List<CourseResponse> listCourses() { return courseService.getAllCourses(); }
    public CourseResponse getCourse(Long id) { return courseService.getCourse(id); }
    public CourseResponse getCourseByCode(String code) { return courseService.getCourseByCode(code); }
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) { return courseService.updateCourse(id, request); }
    public void deleteCourse(Long id) { courseService.deleteCourse(id); }
    public ImportResult importMaterial(Long courseId, String filename, String contentType, byte[] content) {
        return courseMaterialService.importMaterial(courseId, filename, contentType, content);
    }
    public List<DocumentResponse> listMaterials(Long courseId) { return courseMaterialService.listDocuments(courseId); }
    public DocumentResponse getMaterial(Long courseId, Long documentId) { return courseMaterialService.getDocument(courseId, documentId); }
    public List<SearchResult> searchMaterials(Long courseId, String query, Integer topK) {
        return courseSearchService.search(courseId, query, topK);
    }
    public List<SearchResult> searchMaterial(Long courseId, Long documentId, String query, Integer topK) {
        return courseSearchService.searchDocument(courseId, documentId, query, topK);
    }
    public LlmAvailability aiStatus() { return llmClient.checkAvailability(); }
    public GroundedAnswer askQuestion(Long courseId, String question) { return groundedQuestionService.ask(courseId, question); }
    public GroundedAnswer askQuestionAboutMaterial(Long courseId, Long documentId, String question) {
        return groundedQuestionService.askDocument(courseId, documentId, question);
    }
    public Resource loadMaterialFile(Long courseId, Long documentId) { return courseMaterialService.loadFile(courseId, documentId); }
    public SummaryResult summarizeDocument(Long courseId, Long documentId) { return summarizationService.summarizeDocument(courseId, documentId); }
    public SummaryResult summarizeTopic(Long courseId, String topic) { return summarizationService.summarizeTopic(courseId, topic); }
    public List<ExtractedConcept> extractConcepts(Long courseId, Long documentId) { return conceptExtractionService.extract(courseId, documentId); }
    public FlashcardGenerationResult generateFlashcards(Long courseId, Long documentId, Integer count) { return flashcardGenerationService.generate(courseId, documentId, count); }
    public List<FlashcardResponse> listFlashcards(Long courseId) { return flashcardReviewService.list(courseId); }
    public FlashcardReviewResponse reviewFlashcard(Long courseId, Long cardId, String rating) { return flashcardReviewService.review(courseId, cardId, rating); }
    public List<FlashcardReviewResponse> flashcardReviewHistory(Long courseId) { return flashcardReviewService.history(courseId); }
    public QuizGenerationResult generateQuiz(Long courseId, Long documentId, Integer count, java.util.Set<String> types) { return quizGenerationService.generate(courseId, documentId, count, types); }
    public List<QuizResponseDto> listQuizzes(Long courseId) { return quizAttemptService.list(courseId); }
    public QuizResponseDto getQuiz(Long courseId, Long quizId) { return quizAttemptService.get(courseId, quizId); }
    public QuizAttemptResult submitQuiz(Long courseId, Long quizId, java.util.Map<Long,String> answers) { return quizAttemptService.submit(courseId, quizId, answers); }
    public GroundedExplanation explainWrongAnswer(Long courseId, Long quizId, Long questionId, String answer) { return wrongAnswerExplanationService.explain(courseId, quizId, questionId, answer); }
    public String agentAssist(Long courseId, String request) { return agentCoordinator.run(courseId, request); }
}
