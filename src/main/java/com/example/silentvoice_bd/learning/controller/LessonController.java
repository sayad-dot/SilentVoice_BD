package com.example.silentvoice_bd.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.learning.dto.LessonCompletionRequest;
import com.example.silentvoice_bd.learning.dto.LessonProgressResponse;
import com.example.silentvoice_bd.learning.dto.LessonResponse;
import com.example.silentvoice_bd.learning.dto.UserLearningStatsResponse;
import com.example.silentvoice_bd.learning.service.LessonService;

@RestController
@RequestMapping("/api/learning/lessons")
@CrossOrigin(origins = "*")
public class LessonController {

    @Autowired
    private LessonService lessonService;

    @GetMapping
    public ResponseEntity<List<LessonResponse>> getAllLessons(@AuthenticationPrincipal User user) {
        try {
            List<LessonResponse> lessons = lessonService.getAllLessonsWithProgress(user.getId());
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        try {
            LessonResponse lesson = lessonService.getLessonWithProgress(lessonId, user.getId());
            return ResponseEntity.ok(lesson);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/type/{lessonType}")
    public ResponseEntity<List<LessonResponse>> getLessonsByType(
            @PathVariable String lessonType,
            @AuthenticationPrincipal User user) {
        try {
            List<LessonResponse> lessons = lessonService.getLessonsByType(lessonType, user.getId());
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{lessonId}/start")
    public ResponseEntity<String> startLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        try {
            lessonService.startLesson(lessonId, user.getId());
            return ResponseEntity.ok("Lesson started successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start lesson");
        }
    }

    @PostMapping("/{lessonId}/complete")
    public ResponseEntity<String> completeLesson(
            @PathVariable Long lessonId,
            @RequestBody LessonCompletionRequest request,
            @AuthenticationPrincipal User user) {
        try {
            lessonService.completeLesson(lessonId, user.getId(),
                    request.getAccuracyScore(), request.getTimeSpent());
            return ResponseEntity.ok("Lesson completed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to complete lesson");
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<List<LessonProgressResponse>> getUserProgress(@AuthenticationPrincipal User user) {
        try {
            List<LessonProgressResponse> progress = lessonService.getUserProgress(user.getId());
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<UserLearningStatsResponse> getUserStats(@AuthenticationPrincipal User user) {
        try {
            UserLearningStatsResponse stats = lessonService.getUserLearningStats(user.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
