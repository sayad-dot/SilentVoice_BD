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
public class SignDto {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String videoUrl;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating new signs
    public SignDto(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
}
