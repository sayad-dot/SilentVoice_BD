package com.example.silentvoice_bd.ai.controllers;

import com.example.silentvoice_bd.ai.dto.AIProcessingRequest;
import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.ai.services.AIProcessingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
@Validated
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIProcessingService aiProcessingService;

    @PostMapping("/predict/{videoId}")
    public CompletableFuture<ResponseEntity<PredictionResponse>> predictSignLanguageAsync(@PathVariable UUID videoId) {
        logger.info("🚀 Received async prediction request for video: {}", videoId);

        return aiProcessingService.processVideoAsync(videoId)
            .thenApply(response -> {
                if (response.isSuccess()) {
                    logger.info("✅ Async prediction successful for video: {} - '{}' ({:.2f}%)",
                              videoId, response.getPredictedText(), response.getConfidence() * 100);

                    // Log normalization status
                    if (response.isLowConfidence()) {
                        logger.warn("⚠️ Low confidence prediction - possible normalization issue");
                    }

                    return ResponseEntity.ok(response);
                } else {
                    logger.warn("❌ Async prediction failed for video: {}. Error: {}", videoId, response.getError());
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .exceptionally(throwable -> {
                logger.error("💥 Async prediction exception for video: " + videoId, throwable);
                PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    @PostMapping("/predict/{videoId}/sync")
    public ResponseEntity<PredictionResponse> predictSignLanguageSync(@PathVariable UUID videoId) {
        logger.info("🔄 Received sync prediction request for video: {}", videoId);

        try {
            PredictionResponse response = aiProcessingService.processVideoSync(videoId);

            if (response.isSuccess()) {
                logger.info("✅ Sync prediction successful for video: {} - '{}' ({:.2f}%)",
                          videoId, response.getPredictedText(), response.getConfidence() * 100);

                // Enhanced logging for confidence levels
                if (response.isHighConfidence()) {
                    logger.info("🎯 High confidence prediction - normalization likely working correctly");
                } else if (response.isLowConfidence()) {
                    logger.error("❌ Very low confidence - normalization issue suspected");
                }

                return ResponseEntity.ok(response);
            } else {
                logger.warn("❌ Sync prediction failed for video: {}. Error: {}", videoId, response.getError());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("💥 Sync prediction exception for video: " + videoId, e);
            PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<PredictionResponse>> processVideoRequest(@Valid @RequestBody AIProcessingRequest request) {
        logger.info("📝 Received processing request: {}", request.getVideoId());

        if (request.getVideoId() == null) {
            logger.warn("❌ Processing request missing video ID");
            PredictionResponse errorResponse = PredictionResponse.error("Video ID is required");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
        }

        if (request.isEnableAsync()) {
            logger.info("🚀 Processing async request for video: {}", request.getVideoId());
            return predictSignLanguageAsync(request.getVideoId());
        } else {
            logger.info("🔄 Processing sync request for video: {}", request.getVideoId());
            return CompletableFuture.completedFuture(predictSignLanguageSync(request.getVideoId()));
        }
    }

    @GetMapping("/predictions/{videoId}")
    public ResponseEntity<List<SignLanguagePrediction>> getPredictions(@PathVariable UUID videoId) {
        try {
            logger.debug("📋 Fetching predictions for video: {}", videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);

            logger.info("📋 Found {} predictions for video: {}", predictions.size(), videoId);

            // Log confidence statistics
            if (!predictions.isEmpty()) {
                double avgConfidence = predictions.stream()
                    .mapToDouble(p -> p.getConfidenceScore().doubleValue())
                    .average()
                    .orElse(0.0);

                logger.info("📊 Average confidence for video {}: {:.2f}%", videoId, avgConfidence * 100);
            }

            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("💥 Failed to fetch predictions for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/high-confidence")
    public ResponseEntity<List<SignLanguagePrediction>> getHighConfidencePredictions(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0.8") double minConfidence) {
        try {
            logger.debug("🎯 Fetching high confidence predictions for video: {} (min confidence: {:.2f})",
                       videoId, minConfidence);

            List<SignLanguagePrediction> predictions = aiProcessingService.getHighConfidencePredictions(videoId, minConfidence);

            logger.info("🎯 Found {} high confidence predictions for video: {}", predictions.size(), videoId);

            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("💥 Failed to fetch high confidence predictions for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/latest")
    public ResponseEntity<SignLanguagePrediction> getLatestPrediction(@PathVariable UUID videoId) {
        try {
            logger.debug("🔍 Fetching latest prediction for video: {}", videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);

            if (!predictions.isEmpty()) {
                SignLanguagePrediction latest = predictions.get(0); // First is latest due to ORDER BY created_at DESC

                logger.info("📝 Latest prediction for video {}: '{}' ({:.2f}%)",
                          videoId, latest.getPredictedText(), latest.getConfidenceScore().doubleValue() * 100);

                return ResponseEntity.ok(latest);
            } else {
                logger.info("❌ No predictions found for video: {}", videoId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("💥 Failed to fetch latest prediction for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAISystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            logger.debug("🔍 Checking AI system status");

            boolean isReady = aiProcessingService.isAISystemReady();
            Map<String, Object> systemInfo = aiProcessingService.getSystemInfo();

            status.put("status", isReady ? "ready" : "not_ready");
            status.put("message", isReady ? "AI system is ready for processing" : "AI system is not available");
            status.put("timestamp", System.currentTimeMillis());
            status.put("systemInfo", systemInfo);

            logger.info("🤖 AI system status check: {}", isReady ? "READY" : "NOT READY");
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("💥 Error checking AI system status", e);
            status.put("status", "error");
            status.put("message", "Error checking AI system: " + e.getMessage());
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(status);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAIStatistics() {
        try {
            logger.debug("📊 Fetching AI statistics");
            Map<String, Object> stats = aiProcessingService.getStatistics();

            // Log key statistics
            if (stats.containsKey("averageConfidence")) {
                logger.info("📊 System average confidence: {}", stats.get("averageConfidence"));
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("💥 Failed to get AI statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/{videoId}")
    public ResponseEntity<Map<String, Object>> getVideoStatistics(@PathVariable UUID videoId) {
        try {
            logger.debug("📊 Fetching statistics for video: {}", videoId);

            Map<String, Object> stats = new HashMap<>();

            Long predictionCount = aiProcessingService.getPredictionCount(videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);

            stats.put("videoId", videoId);
            stats.put("totalPredictions", predictionCount);
            stats.put("hasHighConfidencePredictions",
                predictions.stream().anyMatch(p -> p.getConfidenceScore().doubleValue() >= 0.8));

            if (!predictions.isEmpty()) {
                SignLanguagePrediction latest = predictions.get(0);
                stats.put("latestPrediction", latest.getPredictedText());
                stats.put("latestConfidence", latest.getConfidenceScore());
                stats.put("latestProcessingTime", latest.getProcessingTimeMs());
                stats.put("latestModelVersion", latest.getModelVersion());
                stats.put("confidenceLevel", latest.getConfidenceScore().doubleValue() > 0.8 ? "HIGH" :
                                           latest.getConfidenceScore().doubleValue() > 0.5 ? "MEDIUM" :
                                           latest.getConfidenceScore().doubleValue() > 0.2 ? "LOW" : "VERY_LOW");

                logger.info("📊 Video {} statistics: {} predictions, latest confidence: {:.2f}%",
                          videoId, predictionCount, latest.getConfidenceScore().doubleValue() * 100);
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("💥 Failed to get video statistics for: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/predictions/{videoId}")
    public ResponseEntity<Map<String, String>> deletePredictions(@PathVariable UUID videoId) {
        try {
            logger.info("🗑️ Deleting predictions for video: {}", videoId);

            aiProcessingService.deletePredictionsByVideoId(videoId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "All predictions deleted for video: " + videoId);
            response.put("videoId", videoId.toString());

            logger.info("✅ Deleted predictions for video: {}", videoId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("💥 Failed to delete predictions for video: " + videoId, e);

            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete predictions: " + e.getMessage());
            response.put("videoId", videoId.toString());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
