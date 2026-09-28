package ca.yorku.eecs3311.coursepilot.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.ArrayList; import java.util.List;
@Entity @Table(name="quizzes") public class Quiz {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id",nullable=false) private Course course;
 @Column(nullable=false,length=255) private String title; @Column(nullable=false) private Instant createdAt;
 @OneToMany(mappedBy="quiz",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") private List<QuizQuestion> questions=new ArrayList<>();
 protected Quiz(){} public Quiz(Course course,String title){this.course=course;this.title=title;} @PrePersist void onCreate(){createdAt=Instant.now();}
 public Long getId(){return id;} public Course getCourse(){return course;} public String getTitle(){return title;} public Instant getCreatedAt(){return createdAt;} public List<QuizQuestion> getQuestions(){return questions;} public void addQuestion(QuizQuestion q){questions.add(q);}
}
