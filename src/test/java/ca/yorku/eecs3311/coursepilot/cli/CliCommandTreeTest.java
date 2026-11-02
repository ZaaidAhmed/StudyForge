package ca.yorku.eecs3311.coursepilot.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CliCommandTreeTest {
    private CommandLine cli;

    @BeforeEach
    void setUp() {
        CoursePilotCommand root = mock(CoursePilotCommand.class);
        when(root.courseCommand()).thenReturn(mock(CourseCommand.class));
        when(root.materialCommand()).thenReturn(mock(MaterialCommand.class));
        when(root.aiCommand()).thenReturn(mock(AiCommand.class));
        when(root.askCommand()).thenReturn(mock(AskCommand.class));
        when(root.summaryCommand()).thenReturn(mock(SummaryCommand.class));
        when(root.conceptsCommand()).thenReturn(mock(ConceptsCommand.class));
        when(root.flashcardsCommand()).thenReturn(mock(FlashcardsCommand.class));
        when(root.quizCommand()).thenReturn(mock(QuizCommand.class));
        when(root.progressCommand()).thenReturn(mock(ProgressCommand.class));
        when(root.studyPlanCommand()).thenReturn(mock(StudyPlanCommand.class));
        when(root.historyCommand()).thenReturn(mock(HistoryCommand.class));
        when(root.recommendationCommand()).thenReturn(mock(RecommendationCommand.class));
        when(root.agentCommand()).thenReturn(mock(AgentCommand.class));
        cli = new CliRunner(null, root).buildCommandLine();
    }

    @Test
    void exposesCoreCommandGroupsAndHelp() {
        var output = new java.io.ByteArrayOutputStream();
        cli.setOut(new java.io.PrintWriter(output, true));
        assertThat(cli.execute("--help")).isZero();
        assertThat(output.toString()).contains("course", "material", "flashcards", "quiz", "agent");
    }

    @Test
    void nestedHelpIsAvailableWithoutApplicationServices() {
        var output = new java.io.ByteArrayOutputStream();
        cli.setOut(new java.io.PrintWriter(output, true));
        assertThat(cli.execute("course", "--help")).isZero();
        assertThat(output.toString()).contains("create", "list", "show");
    }

    @Test
    void missingRequiredCourseCreateOptionsReturnNonZero() {
        var error = new java.io.ByteArrayOutputStream();
        cli.setErr(new java.io.PrintWriter(error, true));
        assertThat(cli.execute("course", "create")).isNotZero();
        assertThat(error.toString()).contains("Missing required option");
    }

}
