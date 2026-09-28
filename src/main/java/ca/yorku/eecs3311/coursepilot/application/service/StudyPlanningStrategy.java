package ca.yorku.eecs3311.coursepilot.application.service;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary; import java.time.*; import java.util.*;
public interface StudyPlanningStrategy {String name();List<Activity> build(LocalDate start,LocalDate target,int minutes,List<ProgressSummary.TopicProgress> topics);record Activity(LocalDate date,String activity,int minutes){}}
