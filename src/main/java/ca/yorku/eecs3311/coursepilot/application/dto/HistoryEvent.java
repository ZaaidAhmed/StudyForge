package ca.yorku.eecs3311.coursepilot.application.dto;
import java.time.Instant;
public record HistoryEvent(Instant occurredAt,String type,String title,String detail){ }
