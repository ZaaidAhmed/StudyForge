package ca.yorku.eecs3311.coursepilot.application.agent; public interface AgentTool {String name(); ToolResult execute(Long courseId); record ToolResult(String tool,boolean success,String output){}}
