package com.example.silentvoice_bd.learning.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "feedback_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "pose_data", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String poseData;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "session_duration")
    private Integer sessionDuration; // in seconds

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
