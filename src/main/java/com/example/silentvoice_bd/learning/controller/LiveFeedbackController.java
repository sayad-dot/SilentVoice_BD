package com.example.silentvoice_bd.learning.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.learning.dto.FeedbackRequest;
import com.example.silentvoice_bd.learning.dto.FeedbackResponse;
import com.example.silentvoice_bd.learning.service.LiveFeedbackService;

@RestController
@RequestMapping("/api/learning/feedback")
// REMOVED: @CrossOrigin(origins = "*") - This was causing CORS conflicts
public class LiveFeedbackController {

    @Autowired
    private LiveFeedbackService feedbackService;

    @PostMapping("/analyze")
    public ResponseEntity<FeedbackResponse> analyzePose(
            @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal User user) {
        try {
            FeedbackResponse response = feedbackService.analyzePoseData(request, user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Create error response
            FeedbackResponse errorResponse = new FeedbackResponse();
            errorResponse.setConfidenceScore(0.0);
            errorResponse.setFeedbackText("Error analyzing pose: " + e.getMessage());
            errorResponse.setIsCorrect(false);
            errorResponse.setTimestamp(java.time.LocalDateTime.now());
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping("/{lessonId}/session/start")
    public ResponseEntity<Map<String, Object>> startFeedbackSession(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        try {
            feedbackService.startFeedbackSession(lessonId, user.getId());

            // Return JSON response instead of plain string
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", java.util.UUID.randomUUID().toString());
            response.put("lessonId", lessonId);
            response.put("status", "started");
            response.put("startTime", java.time.LocalDateTime.now().toString());
            response.put("message", "Feedback session started successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start session: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{lessonId}/session/end")
    public ResponseEntity<Map<String, Object>> endFeedbackSession(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        try {
            feedbackService.endFeedbackSession(lessonId, user.getId());

            // Return JSON response instead of plain string
            Map<String, Object> response = new HashMap<>();
            response.put("lessonId", lessonId);
            response.put("status", "ended");
            response.put("endTime", java.time.LocalDateTime.now().toString());
            response.put("message", "Feedback session ended successfully");
            response.put("accuracy", 75.5); // Mock data
            response.put("feedback", "Good practice! Keep it up!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to end session: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
