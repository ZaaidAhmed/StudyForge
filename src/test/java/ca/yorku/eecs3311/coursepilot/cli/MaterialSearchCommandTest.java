package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.application.service.CourseNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MaterialSearchCommandTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream output;
    private StudyAgentFacade facade;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        facade = mock(StudyAgentFacade.class);
    }

    @AfterEach
    void tearDown() { System.setOut(originalOut); }

    @Test
    void searchCommandPrintsCourseScopedEvidence() {
        when(facade.getCourseByCode("EECS3311")).thenReturn(new CourseResponse(1L, "EECS3311", "Software Design", null, null, null, null));
        when(facade.searchMaterials(1L, "strategy", 5)).thenReturn(List.of(
                new SearchResult(7L, 9L, "notes.pdf", 2, "Strategy pattern evidence", 4.5, 12, 12, List.of("strategy"))));

        var parent = new MaterialCommand(facade);
        int exit = new CommandLine(new MaterialSearchCommand(parent)).execute("--course", "EECS3311", "--query", "strategy", "--limit", "5");

        assertThat(exit).isZero();
        assertThat(output.toString()).contains("EECS3311", "notes.pdf", "Page: 12", "Strategy pattern evidence");
        verify(facade).searchMaterials(1L, "strategy", 5);
    }

    @Test
    void missingCourseIsNotSilentlyConvertedToSuccess() {
        when(facade.getCourseByCode("MISSING")).thenThrow(new CourseNotFoundException("Course not found."));

        var parent = new MaterialCommand(facade);
        var error = new ByteArrayOutputStream();
        var command = new CommandLine(new MaterialSearchCommand(parent));
        command.setErr(new java.io.PrintWriter(error, true));
        int exit = command.execute("--course", "MISSING", "--query", "strategy");
        assertThat(exit).isNotZero();
    }
}
