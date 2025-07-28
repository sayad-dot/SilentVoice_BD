package com.example.silentvoice_bd.controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.silentvoice_bd.service.LiveProcessingService;

@Controller
public class LiveStreamController {

    private static final Logger logger = LoggerFactory.getLogger(LiveStreamController.class);

    @Autowired
    private LiveProcessingService liveProcessingService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/start-live-session")
    @SendTo("/topic/session-status")
    public Map<String, Object> startLiveSession(Map<String, String> sessionData) {
        try {
            logger.info("🎬 Starting live session for user: {}", sessionData.get("userId"));

            String sessionId = liveProcessingService.initializeLiveSession(
                    sessionData.get("userId")
            );

            Map<String, Object> response = Map.of(
                    "sessionId", sessionId,
                    "status", "started",
                    "message", "Live recognition session started successfully",
                    "timestamp", System.currentTimeMillis()
            );

            logger.info("✅ Live session created with ID: {}", sessionId);
            return response;

        } catch (Exception e) {
            logger.error("❌ Failed to start live session", e);
            return Map.of(
                    "status", "error",
                    "error", true,
                    "message", "Failed to start session: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    @MessageMapping("/live-frame")
    public void processLiveFrame(Map<String, Object> frameData, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String aiSessionId = (String) frameData.get("sessionId");
            String frameDataBase64 = (String) frameData.get("frameData");
            Long timestamp = (Long) frameData.get("timestamp");

            // Get the WebSocket session ID for proper user routing
            String wsSessionId = headerAccessor.getSessionId();

            logger.debug("📹 Processing frame for AI session: {} (WebSocket: {})", aiSessionId, wsSessionId);

            // Add timeout protection
            CompletableFuture<Void> processingTask = CompletableFuture.runAsync(() -> {
                try {
                    liveProcessingService.processFrameAsync(aiSessionId, frameDataBase64, timestamp,
                            (prediction) -> {
                                logger.debug("🔮 Sending prediction to topic for session {}: {}",
                                        aiSessionId, prediction.get("prediction"));

                                // Send to topic using AI session ID (FIXED)
                                messagingTemplate.convertAndSend(
                                        "/topic/predictions." + aiSessionId,
                                        prediction
                                );
                            });
                } catch (Exception e) {
                    logger.error("❌ Frame processing error for AI session {}", aiSessionId, e);

                    // Send error to topic (FIXED)
                    messagingTemplate.convertAndSend(
                            "/topic/errors." + aiSessionId,
                            Map.of(
                                    "error", true,
                                    "message", "Frame processing failed: " + e.getMessage(),
                                    "aiSessionId", aiSessionId,
                                    "timestamp", System.currentTimeMillis()
                            )
                    );
                }
            });

            // Set 30-second timeout for processing
            try {
                processingTask.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("⏰ Frame processing timeout for AI session {}", aiSessionId);
                processingTask.cancel(true);

                // Send timeout error to topic (FIXED)
                messagingTemplate.convertAndSend("/topic/errors." + aiSessionId,
                        Map.of(
                                "error", true,
                                "message", "Processing timeout - system overloaded",
                                "aiSessionId", aiSessionId,
                                "timestamp", System.currentTimeMillis()
                        )
                );
            }

        } catch (Exception e) {
            logger.error("❌ Error processing frame", e);
            String aiSessionId = (String) frameData.get("sessionId");

            // Send general error to topic (FIXED)
            messagingTemplate.convertAndSend("/topic/errors." + aiSessionId,
                    Map.of(
                            "error", true,
                            "message", "Processing failed: " + e.getMessage(),
                            "aiSessionId", aiSessionId,
                            "timestamp", System.currentTimeMillis()
                    )
            );
        }
    }

    @MessageMapping("/stop-live-session")
    @SendTo("/topic/session-status")
    public Map<String, Object> stopLiveSession(Map<String, String> sessionData) {
        try {
            String sessionId = sessionData.get("sessionId");
            logger.info("🛑 Stopping live session: {}", sessionId);

            // Clean up session resources
            liveProcessingService.cleanupSession(sessionId);

            logger.info("Live session {} stopped by user request", sessionId);

            return Map.of(
                    "sessionId", sessionId,
                    "status", "stopped",
                    "message", "Live session stopped successfully",
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            logger.error("❌ Failed to stop live session", e);
            return Map.of(
                    "status", "error",
                    "error", true,
                    "message", "Failed to stop session: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}
