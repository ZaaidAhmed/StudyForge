package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @Column(name = "course_code", nullable = false, unique = true, length = 32)
    private String courseCode;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    public Course(String courseCode, String name, String description, LocalDate examDate) {
        this.courseCode = courseCode;
        this.name = name;
        this.description = description;
        this.examDate = examDate;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getExamDate() { return examDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String courseCode, String name, String description, LocalDate examDate) {
        this.courseCode = courseCode;
        this.name = name;
        this.description = description;
        this.examDate = examDate;
    }
}
