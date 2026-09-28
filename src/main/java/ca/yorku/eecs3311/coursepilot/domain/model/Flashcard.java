package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flashcards")
public class Flashcard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @Column(nullable = false, length = 1000) private String question;
    @Column(nullable = false, length = 3000) private String answer;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @OneToMany(mappedBy = "flashcard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") private List<FlashcardSource> sources = new ArrayList<>();
    protected Flashcard() { }
    public Flashcard(Course course, String question, String answer) { this.course = course; this.question = question; this.answer = answer; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; } public Course getCourse() { return course; }
    public String getQuestion() { return question; } public String getAnswer() { return answer; }
    public Instant getCreatedAt() { return createdAt; } public List<FlashcardSource> getSources() { return sources; }
    public void addSource(FlashcardSource source) { sources.add(source); }
}
