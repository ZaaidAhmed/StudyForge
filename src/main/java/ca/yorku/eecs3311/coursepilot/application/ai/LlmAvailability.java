package ca.yorku.eecs3311.coursepilot.application.ai;

public record LlmAvailability(LlmAvailabilityStatus status, String provider, String model, String message) {
    public boolean isAvailable() { return status == LlmAvailabilityStatus.AVAILABLE; }
}
