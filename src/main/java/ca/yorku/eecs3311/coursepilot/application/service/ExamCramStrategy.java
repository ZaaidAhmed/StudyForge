package ca.yorku.eecs3311.coursepilot.application.service;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary; import java.time.*; import java.util.*;
public class ExamCramStrategy implements StudyPlanningStrategy {public String name(){return "EXAM_CRAM";}public List<Activity> build(LocalDate s,LocalDate t,int m,List<ProgressSummary.TopicProgress> topics){var weak=topics.stream().sorted(Comparator.comparingDouble(ProgressSummary.TopicProgress::score)).toList();return BalancedStudyStrategy.distribute(s,t,m,weak,"Rapid review of");}}
