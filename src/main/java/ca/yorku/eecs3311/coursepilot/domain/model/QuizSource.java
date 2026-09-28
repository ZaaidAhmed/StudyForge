package ca.yorku.eecs3311.coursepilot.domain.model;
import jakarta.persistence.*;
@Entity @Table(name="quiz_sources") public class QuizSource {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="question_id",nullable=false) private QuizQuestion question;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="document_id",nullable=false) private CourseDocument document; @Column(nullable=false) private Long chunkId; @Column(nullable=false) private int chunkIndex; private Integer pageStart; private Integer pageEnd;
 protected QuizSource(){} public QuizSource(QuizQuestion q,CourseDocument d,Long chunkId,int chunkIndex,Integer start,Integer end){this.question=q;this.document=d;this.chunkId=chunkId;this.chunkIndex=chunkIndex;this.pageStart=start;this.pageEnd=end;} public Long getId(){return id;} public CourseDocument getDocument(){return document;} public Long getChunkId(){return chunkId;} public int getChunkIndex(){return chunkIndex;} public Integer getPageStart(){return pageStart;} public Integer getPageEnd(){return pageEnd;}
}
