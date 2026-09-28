package ca.yorku.eecs3311.coursepilot.cli;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "status", description = "Check Ollama availability", mixinStandardHelpOptions = true)
public class AiStatusCommand implements Runnable {
    private final AiCommand parent;
    public AiStatusCommand(AiCommand parent) { this.parent = parent; }

    @Override public void run() {
        LlmAvailability availability = parent.facade().aiStatus();
        System.out.println("AI Provider: " + availability.provider());
        System.out.println("Model: " + availability.model());
        System.out.println("Status: " + availability.status());
        System.out.println("Message: " + availability.message());
    }
}
