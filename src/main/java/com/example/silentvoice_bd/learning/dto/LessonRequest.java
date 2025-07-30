package com.example.silentvoice_bd.learning.dto;

import lombok.Data;

@Data
public class LessonRequest {

    private String title;
    private String description;
    private String lessonType; // ALPHABET, WORD, PHRASE
    private String contentData; // JSON string
    private Integer difficultyLevel;
    private Integer estimatedDuration;
}
