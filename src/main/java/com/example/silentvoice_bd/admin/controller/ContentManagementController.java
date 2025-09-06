package com.example.silentvoice_bd.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.admin.dto.LessonDto;
import com.example.silentvoice_bd.admin.dto.SignDto;
import com.example.silentvoice_bd.admin.service.ContentManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContentManagementController {

    private final ContentManagementService contentManagementService;

    // ===== LESSON ENDPOINTS =====
    /**
     * Get all lessons
     */
    @GetMapping("/lessons")
    public ResponseEntity<List<LessonDto>> getAllLessons() {
        try {
            List<LessonDto> lessons = contentManagementService.getAllLessons();
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new lesson
     */
    @PostMapping(value = "/lessons", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LessonDto> createLesson(@RequestBody LessonDto lessonDto) {
        try {
            LessonDto createdLesson = contentManagementService.createLesson(lessonDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdLesson);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing lesson
     */
    @PutMapping(value = "/lessons/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonDto lessonDto) {
        try {
            LessonDto updatedLesson = contentManagementService.updateLesson(id, lessonDto);
            return ResponseEntity.ok(updatedLesson);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a lesson
     */
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        try {
            contentManagementService.deleteLesson(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== SIGN ENDPOINTS =====
    /**
     * Get all signs
     */
    @GetMapping("/signs")
    public ResponseEntity<List<SignDto>> getAllSigns() {
        try {
            List<SignDto> signs = contentManagementService.getAllSigns();
            return ResponseEntity.ok(signs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new sign with file uploads
     */
    @PostMapping(value = "/signs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignDto> createSign(
            @RequestPart("sign") SignDto signDto,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            SignDto createdSign = contentManagementService.createSign(signDto, video, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSign);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing sign with optional file uploads
     */
    @PutMapping(value = "/signs/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignDto> updateSign(
            @PathVariable Long id,
            @RequestPart("sign") SignDto signDto,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            SignDto updatedSign = contentManagementService.updateSign(id, signDto, video, image);
            return ResponseEntity.ok(updatedSign);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a sign
     */
    @DeleteMapping("/signs/{id}")
    public ResponseEntity<Void> deleteSign(@PathVariable Long id) {
        try {
            contentManagementService.deleteSign(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== STATISTICS ENDPOINT =====
    /**
     * Get content statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getContentStatistics() {
        try {
            Map<String, Object> statistics = contentManagementService.getContentStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== BULK OPERATIONS =====
    /**
     * Bulk delete lessons
     */
    @DeleteMapping("/lessons/bulk")
    public ResponseEntity<Void> bulkDeleteLessons(@RequestBody List<Long> lessonIds) {
        try {
            lessonIds.forEach(contentManagementService::deleteLesson);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bulk delete signs
     */
    @DeleteMapping("/signs/bulk")
    public ResponseEntity<Void> bulkDeleteSigns(@RequestBody List<Long> signIds) {
        try {
            signIds.forEach(contentManagementService::deleteSign);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== SEARCH & FILTER ENDPOINTS =====
    /**
     * Search lessons by category
     */
    @GetMapping("/lessons/category/{category}")
    public ResponseEntity<List<LessonDto>> getLessonsByCategory(@PathVariable String category) {
        try {
            List<LessonDto> lessons = contentManagementService.getAllLessons()
                    .stream()
                    .filter(lesson -> category.equalsIgnoreCase(lesson.getCategory()))
                    .toList();
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search lessons by difficulty
     */
    @GetMapping("/lessons/difficulty/{difficulty}")
    public ResponseEntity<List<LessonDto>> getLessonsByDifficulty(@PathVariable String difficulty) {
        try {
            List<LessonDto> lessons = contentManagementService.getAllLessons()
                    .stream()
                    .filter(lesson -> difficulty.equalsIgnoreCase(lesson.getDifficulty()))
                    .toList();
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search signs by category
     */
    @GetMapping("/signs/category/{category}")
    public ResponseEntity<List<SignDto>> getSignsByCategory(@PathVariable String category) {
        try {
            List<SignDto> signs = contentManagementService.getAllSigns()
                    .stream()
                    .filter(sign -> category.equalsIgnoreCase(sign.getCategory()))
                    .toList();
            return ResponseEntity.ok(signs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== HEALTH CHECK =====
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "content-management",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
