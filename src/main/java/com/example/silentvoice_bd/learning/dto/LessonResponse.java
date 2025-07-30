package com.example.silentvoice_bd.learning.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonResponse {

    private Long id;
    private String title;
    private String description;
    private String lessonType;
    private Integer difficultyLevel;
    private Integer estimatedDuration;
    private String contentData;

    // Progress fields
    private String status;
    private Double accuracyScore;
    private Integer attempts;
    private Integer timeSpent;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
