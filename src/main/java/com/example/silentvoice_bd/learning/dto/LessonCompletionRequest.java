package com.example.silentvoice_bd.learning.dto;

import lombok.Data;

@Data
public class LessonCompletionRequest {

    private Double accuracyScore;
    private Integer timeSpent;
}
