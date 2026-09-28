package ca.yorku.eecs3311.coursepilot.domain.model;
import jakarta.persistence.*; import java.util.ArrayList; import java.util.List;
@Entity @Table(name="quiz_questions") public class QuizQuestion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="quiz_id",nullable=false) private Quiz quiz;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private QuizQuestionType type; @Column(nullable=false,length=1000) private String prompt; @Column(name="options_json",length=3000) private String optionsJson;
 @Column(name="correct_answer",length=500) private String correctAnswer; @Column(name="reference_answer",length=3000) private String referenceAnswer;
 @OneToMany(mappedBy="question",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") private List<QuizSource> sources=new ArrayList<>(); protected QuizQuestion(){}
 public QuizQuestion(Quiz quiz,QuizQuestionType type,String prompt,String optionsJson,String correctAnswer,String referenceAnswer){this.quiz=quiz;this.type=type;this.prompt=prompt;this.optionsJson=optionsJson;this.correctAnswer=correctAnswer;this.referenceAnswer=referenceAnswer;}
 public Long getId(){return id;} public Quiz getQuiz(){return quiz;} public QuizQuestionType getType(){return type;} public String getPrompt(){return prompt;} public String getOptionsJson(){return optionsJson;} public String getCorrectAnswer(){return correctAnswer;} public String getReferenceAnswer(){return referenceAnswer;} public List<QuizSource> getSources(){return sources;} public void addSource(QuizSource s){sources.add(s);}
}
