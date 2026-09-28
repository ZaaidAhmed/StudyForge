package ca.yorku.eecs3311.coursepilot.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "studyforge", description = "StudyForge AI command-line interface", mixinStandardHelpOptions = true)
public class CoursePilotCommand implements Runnable {
    private final CourseCommand courseCommand;
    private final MaterialCommand materialCommand;
    private final AiCommand aiCommand;
    private final AskCommand askCommand;
    private final SummaryCommand summaryCommand;
    private final ConceptsCommand conceptsCommand;
    private final FlashcardsCommand flashcardsCommand;
    private final QuizCommand quizCommand;
    private final ProgressCommand progressCommand;
    private final StudyPlanCommand studyPlanCommand;
    private final HistoryCommand historyCommand;
    private final RecommendationCommand recommendationCommand;
    private final AgentCommand agentCommand;

    public CoursePilotCommand(CourseCommand courseCommand, MaterialCommand materialCommand, AiCommand aiCommand, AskCommand askCommand, SummaryCommand summaryCommand, ConceptsCommand conceptsCommand, FlashcardsCommand flashcardsCommand, QuizCommand quizCommand, ProgressCommand progressCommand, StudyPlanCommand studyPlanCommand, HistoryCommand historyCommand, RecommendationCommand recommendationCommand, AgentCommand agentCommand) {
        this.courseCommand = courseCommand;
        this.materialCommand = materialCommand;
        this.aiCommand = aiCommand;
        this.askCommand = askCommand;
        this.summaryCommand = summaryCommand;
        this.conceptsCommand = conceptsCommand;
        this.flashcardsCommand = flashcardsCommand;
        this.quizCommand = quizCommand;
        this.progressCommand = progressCommand;
        this.studyPlanCommand = studyPlanCommand;
        this.historyCommand = historyCommand;
        this.recommendationCommand = recommendationCommand;
        this.agentCommand = agentCommand;
    }

    @Override
    public void run() { System.out.println("Use 'studyforge course --help' to manage courses."); }

    public CourseCommand courseCommand() { return courseCommand; }
    public MaterialCommand materialCommand() { return materialCommand; }
    public AiCommand aiCommand() { return aiCommand; }
    public AskCommand askCommand() { return askCommand; }
    public SummaryCommand summaryCommand() { return summaryCommand; }
    public ConceptsCommand conceptsCommand() { return conceptsCommand; }
    public FlashcardsCommand flashcardsCommand() { return flashcardsCommand; }
    public QuizCommand quizCommand() { return quizCommand; }
    public ProgressCommand progressCommand() { return progressCommand; }
    public StudyPlanCommand studyPlanCommand() { return studyPlanCommand; }
    public HistoryCommand historyCommand() { return historyCommand; }
    public RecommendationCommand recommendationCommand() { return recommendationCommand; }
    public AgentCommand agentCommand() { return agentCommand; }
}
