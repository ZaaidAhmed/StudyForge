package ca.yorku.eecs3311.coursepilot.application.dto;
import ca.yorku.eecs3311.coursepilot.domain.model.*; import java.time.Instant; import java.util.*;
public record QuizResponseDto(Long id,String title,Long courseId,Instant createdAt,List<QuizQuestionResponse> questions){public static QuizResponseDto from(Quiz q){return new QuizResponseDto(q.getId(),q.getTitle(),q.getCourse().getId(),q.getCreatedAt(),q.getQuestions().stream().map(QuizQuestionResponse::from).toList());}}
