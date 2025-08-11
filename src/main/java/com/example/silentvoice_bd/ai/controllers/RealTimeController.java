package com.example.silentvoice_bd.ai.controllers;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.silentvoice_bd.ai.services.RealTimeProcessingService;

@RestController
@RequestMapping("/api/realtime")
public class RealTimeController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeController.class);

    @Autowired
    private RealTimeProcessingService realTimeService;

    /**
     * Create a new real-time processing session. Returns a sessionId to use for
     * frame uploads.
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> createSession() {
        String sessionId = UUID.randomUUID().toString();
        logger.info("🔄 Creating real-time session: {}", sessionId);

        Map<String, Object> result = realTimeService.createSession(sessionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Process a single base64-encoded frame in the given session. Expects JSON:
     * { "frame": "<base64-string>" }
     */
    @PostMapping("/session/{sessionId}/frame")
    public ResponseEntity<Map<String, Object>> processFrame(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> payload
    ) {
        String base64Frame = payload.get("frame");
        if (base64Frame == null || base64Frame.isEmpty()) {
            logger.warn("⚠️ Missing frame data for session {}", sessionId);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Frame data is required"));
        }

        try {
            Map<String, Object> result = realTimeService.processFrame(sessionId, base64Frame);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("💥 Error processing frame for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get current statistics for a session: frames processed, predictions made,
     * etc.
     */
    @GetMapping("/session/{sessionId}/stats")
    public ResponseEntity<Map<String, Object>> getSessionStats(@PathVariable String sessionId) {
        try {
            Map<String, Object> stats = realTimeService.getSessionStats(sessionId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("💥 Error fetching stats for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Close and delete a real-time session.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> closeSession(@PathVariable String sessionId) {
        try {
            Map<String, Object> result = realTimeService.closeSession(sessionId);
            logger.info("🗑️ Closed real-time session: {}", sessionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("💥 Error closing session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
