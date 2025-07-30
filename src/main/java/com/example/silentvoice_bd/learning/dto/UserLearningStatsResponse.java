package com.example.silentvoice_bd.learning.dto;

import lombok.Data;

@Data
public class UserLearningStatsResponse {

    private Long completedLessons;
    private Double averageAccuracy;
    private Long totalTimeSpent; // in seconds
    private Long totalSessions;
    private String learningStreak;
}
