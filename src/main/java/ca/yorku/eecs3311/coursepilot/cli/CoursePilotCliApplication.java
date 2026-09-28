package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.CoursePilotApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class CoursePilotCliApplication {
    private CoursePilotCliApplication() { }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CoursePilotApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("cli")
                .run(args);
    }
}
