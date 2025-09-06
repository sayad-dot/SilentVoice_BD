package com.example.silentvoice_bd.admin.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.admin.dto.LessonDto;
import com.example.silentvoice_bd.admin.dto.SignDto;
import com.example.silentvoice_bd.learning.model.Lesson;
import com.example.silentvoice_bd.learning.model.Sign;
import com.example.silentvoice_bd.learning.repository.LessonRepository;
import com.example.silentvoice_bd.learning.repository.SignRepository;
import com.example.silentvoice_bd.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentManagementService {

    private final LessonRepository lessonRepository;
    private final SignRepository signRepository;
    private final FileStorageService fileStorageService;

    // ===== LESSON MANAGEMENT =====
    public List<LessonDto> getAllLessons() {
        return lessonRepository.findAll()
                .stream()
                .map(this::convertToLessonDto)
                .collect(Collectors.toList());
    }

    public LessonDto createLesson(LessonDto lessonDto) {
        Lesson lesson = new Lesson();
        lesson.setTitle(lessonDto.getTitle());
        lesson.setDescription(lessonDto.getDescription());

        // Map DTO fields to your existing Lesson entity fields
        lesson.setLessonType(lessonDto.getCategory()); // category -> lessonType
        lesson.setDifficultyLevel(mapDifficultyStringToInteger(lessonDto.getDifficulty())); // difficulty -> difficultyLevel

        // Convert Instant to LocalDateTime for your entity
        lesson.setCreatedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        lesson.setUpdatedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));

        Lesson saved = lessonRepository.save(lesson);
        return convertToLessonDto(saved);
    }

    public LessonDto updateLesson(Long id, LessonDto lessonDto) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        lesson.setTitle(lessonDto.getTitle());
        lesson.setDescription(lessonDto.getDescription());

        // Map DTO fields to your existing Lesson entity fields
        lesson.setLessonType(lessonDto.getCategory()); // category -> lessonType
        lesson.setDifficultyLevel(mapDifficultyStringToInteger(lessonDto.getDifficulty())); // difficulty -> difficultyLevel

        lesson.setUpdatedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));

        Lesson updated = lessonRepository.save(lesson);
        return convertToLessonDto(updated);
    }

    public void deleteLesson(Long id) {
        lessonRepository.deleteById(id);
    }

    // ===== SIGN MANAGEMENT =====
    public List<SignDto> getAllSigns() {
        return signRepository.findAll()
                .stream()
                .map(this::convertToSignDto)
                .collect(Collectors.toList());
    }

    public SignDto createSign(SignDto signDto, MultipartFile video, MultipartFile image) {
        Sign sign = new Sign();
        sign.setName(signDto.getName());
        sign.setDescription(signDto.getDescription());
        sign.setCategory(signDto.getCategory());
        sign.setCreatedAt(Instant.now());
        sign.setUpdatedAt(Instant.now());

        // Handle file uploads
        if (video != null && !video.isEmpty()) {
            try {
                String videoUrl = fileStorageService.storeFile(video, "signs/videos");
                sign.setVideoUrl(videoUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store video file", e);
            }
        }

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileStorageService.storeFile(image, "signs/images");
                sign.setImageUrl(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store image file", e);
            }
        }

        Sign saved = signRepository.save(sign);
        return convertToSignDto(saved);
    }

    public SignDto updateSign(Long id, SignDto signDto, MultipartFile video, MultipartFile image) {
        Sign sign = signRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sign not found"));

        sign.setName(signDto.getName());
        sign.setDescription(signDto.getDescription());
        sign.setCategory(signDto.getCategory());
        sign.setUpdatedAt(Instant.now());

        // Handle file uploads
        if (video != null && !video.isEmpty()) {
            try {
                String videoUrl = fileStorageService.storeFile(video, "signs/videos");
                sign.setVideoUrl(videoUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store video file", e);
            }
        }

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileStorageService.storeFile(image, "signs/images");
                sign.setImageUrl(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store image file", e);
            }
        }

        Sign updated = signRepository.save(sign);
        return convertToSignDto(updated);
    }

    public void deleteSign(Long id) {
        signRepository.deleteById(id);
    }

    // ===== STATISTICS ===== 
    public Map<String, Object> getContentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalLessons", lessonRepository.count());

        // Use total count instead of the removed countByIsActive method:
        stats.put("activeLessons", lessonRepository.count());

        stats.put("totalSigns", signRepository.count());
        stats.put("signsByCategory", getSignsByCategory());
        stats.put("lessonsByDifficulty", getLessonsByDifficulty());

        return stats;
    }

    // Helper methods
    private LessonDto convertToLessonDto(Lesson lesson) {
        return LessonDto.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .category(lesson.getLessonType()) // lessonType -> category
                .difficulty(mapDifficultyIntegerToString(lesson.getDifficultyLevel())) // difficultyLevel -> difficulty
                .orderIndex(1) // Default value since your entity doesn't have this field
                .isActive(true) // Default to true since your entity doesn't have this field
                .createdAt(lesson.getCreatedAt() != null
                        ? lesson.getCreatedAt().toInstant(ZoneOffset.UTC) : Instant.now())
                .updatedAt(lesson.getUpdatedAt() != null
                        ? lesson.getUpdatedAt().toInstant(ZoneOffset.UTC) : Instant.now())
                .build();
    }

    private SignDto convertToSignDto(Sign sign) {
        return SignDto.builder()
                .id(sign.getId())
                .name(sign.getName())
                .description(sign.getDescription())
                .category(sign.getCategory())
                .videoUrl(sign.getVideoUrl())
                .imageUrl(sign.getImageUrl())
                .createdAt(sign.getCreatedAt())
                .updatedAt(sign.getUpdatedAt())
                .build();
    }

    private Map<String, Long> getSignsByCategory() {
        return signRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        Sign::getCategory,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getLessonsByDifficulty() {
        return lessonRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        lesson -> mapDifficultyIntegerToString(lesson.getDifficultyLevel()),
                        Collectors.counting()
                ));
    }

    // Utility methods to convert between String and Integer difficulty levels
    private Integer mapDifficultyStringToInteger(String difficulty) {
        if (difficulty == null) {
            return 1;
        }

        return switch (difficulty.toLowerCase()) {
            case "beginner", "easy" ->
                1;
            case "intermediate", "medium" ->
                2;
            case "advanced", "hard" ->
                3;
            default ->
                1;
        };
    }

    private String mapDifficultyIntegerToString(Integer difficultyLevel) {
        if (difficultyLevel == null) {
            return "Beginner";
        }

        return switch (difficultyLevel) {
            case 1 ->
                "Beginner";
            case 2 ->
                "Intermediate";
            case 3 ->
                "Advanced";
            default ->
                "Beginner";
        };
    }
}
