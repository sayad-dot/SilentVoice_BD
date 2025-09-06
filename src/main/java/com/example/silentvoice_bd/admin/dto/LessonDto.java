package com.example.silentvoice_bd.admin.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDto {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String difficulty;
    private Integer orderIndex;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
