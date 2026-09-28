package ca.yorku.eecs3311.coursepilot.application.dto;
import java.util.List;
public record ProgressSummary(Long courseId,String courseName,int activityCount,double overallScore,String overallStatus,List<TopicProgress> topics){public ProgressSummary{topics=List.copyOf(topics);} public boolean hasActivity(){return activityCount>0;} public record TopicProgress(String topic,double score,String status,int activities){} }
