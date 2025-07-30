package com.example.silentvoice_bd.learning.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LessonProgressResponse {

    private Long lessonId;
    private String lessonTitle;
    private String status;
    private Double accuracyScore;
    private Integer attempts;
    private Integer timeSpent;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
