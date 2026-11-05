package ca.yorku.eecs3311.coursepilot.application.agent;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentCoordinator {
    private final IntentAnalyzer analyzer;
    private final TaskPlanner planner;
    private final AgentPlanValidator validator;
    private final AgentToolManager tools;
    private final MemoryManager memory;
    private final ResponseSynthesizer synthesizer;

    public AgentCoordinator(IntentAnalyzer analyzer, TaskPlanner planner, AgentPlanValidator validator,
                            AgentToolManager tools, MemoryManager memory, ResponseSynthesizer synthesizer) {
        this.analyzer = analyzer;
        this.planner = planner;
        this.validator = validator;
        this.tools = tools;
        this.memory = memory;
        this.synthesizer = synthesizer;
    }

    public String run(Long courseId, String request) {
        if (courseId == null) throw new IllegalArgumentException("A course is required for agent assistance.");
        AgentIntent intent = analyzer.analyze(request);
        if (intent == AgentIntent.UNSUPPORTED_REQUEST) {
            return synthesizer.synthesize(intent, List.of(), List.of());
        }
        AgentPlan plan = planner.plan(intent, request);
        validator.validate(plan, courseId);
        List<AgentTool.ToolResult> results = new ArrayList<>();
        for (AgentPlanStep step : plan.steps()) results.add(tools.execute(step, courseId));
        return synthesizer.synthesize(intent, results, memory.recent(courseId));
    }
}
