package ca.yorku.eecs3311.coursepilot.domain.repository;
import ca.yorku.eecs3311.coursepilot.domain.model.Quiz;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface QuizRepository extends JpaRepository<Quiz,Long> {
    @Query("select distinct q from Quiz q left join fetch q.questions where q.course.id=:courseId order by q.id asc")
    List<Quiz> findByCourseId(@Param("courseId")Long courseId);

    @Query("select distinct q from Quiz q left join fetch q.questions where q.id=:id and q.course.id=:courseId")
    Optional<Quiz> findOwned(@Param("id")Long id,@Param("courseId")Long courseId);

    @Query("select distinct q from Quiz q left join fetch q.questions " +
            "where q.course.id=:courseId and exists (select qs.id from QuizQuestion qs join qs.sources link " +
            "where qs.quiz=q and link.document.id=:documentId) order by q.id")
    List<Quiz> findByCourseIdAndSourceDocumentId(@Param("courseId") Long courseId,
                                                  @Param("documentId") Long documentId);

    @Query("select distinct q from Quiz q left join fetch q.questions where q.course.id=:courseId and q.creationOrigin=:origin")
    List<Quiz> findByCourseIdAndCreationOrigin(@Param("courseId") Long courseId,
                                                @Param("origin") ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin origin);
}
