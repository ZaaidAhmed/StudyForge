package ca.yorku.eecs3311.coursepilot.domain.repository;
import ca.yorku.eecs3311.coursepilot.domain.model.QuizAttempt; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt,Long>{ @Query("select a from QuizAttempt a join fetch a.quiz q where q.course.id=:courseId order by a.id asc") List<QuizAttempt> findByCourseId(@Param("courseId")Long courseId); }
