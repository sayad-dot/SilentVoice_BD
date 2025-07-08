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
        logger.info("Received async prediction request for video: {}", videoId);

        return aiProcessingService.processVideoAsync(videoId)
            .thenApply(response -> {
                if (response.isSuccess()) {
                    logger.info("Async prediction successful for video: {}", videoId);
                    return ResponseEntity.ok(response);
                } else {
                    logger.warn("Async prediction failed for video: {}. Error: {}", videoId, response.getError());
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Async prediction exception for video: " + videoId, throwable);
                PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    @PostMapping("/predict/{videoId}/sync")
    public ResponseEntity<PredictionResponse> predictSignLanguageSync(@PathVariable UUID videoId) {
        logger.info("Received sync prediction request for video: {}", videoId);

        try {
            PredictionResponse response = aiProcessingService.processVideoSync(videoId);

            if (response.isSuccess()) {
                logger.info("Sync prediction successful for video: {}", videoId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Sync prediction failed for video: {}. Error: {}", videoId, response.getError());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Sync prediction exception for video: " + videoId, e);
            PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<PredictionResponse>> processVideoRequest(@Valid @RequestBody AIProcessingRequest request) {
        logger.info("Received processing request: {}", request.getVideoId());

        if (request.getVideoId() == null) {
            PredictionResponse errorResponse = PredictionResponse.error("Video ID is required");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
        }

        if (request.isEnableAsync()) {
            return predictSignLanguageAsync(request.getVideoId());
        } else {
            return CompletableFuture.completedFuture(predictSignLanguageSync(request.getVideoId()));
        }
    }

    @GetMapping("/predictions/{videoId}")
    public ResponseEntity<List<SignLanguagePrediction>> getPredictions(@PathVariable UUID videoId) {
        try {
            logger.debug("Fetching predictions for video: {}", videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("Failed to fetch predictions for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/high-confidence")
    public ResponseEntity<List<SignLanguagePrediction>> getHighConfidencePredictions(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0.8") double minConfidence) {
        try {
            logger.debug("Fetching high confidence predictions for video: {} (min confidence: {})", videoId, minConfidence);
            List<SignLanguagePrediction> predictions = aiProcessingService.getHighConfidencePredictions(videoId, minConfidence);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("Failed to fetch high confidence predictions for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/latest")
    public ResponseEntity<SignLanguagePrediction> getLatestPrediction(@PathVariable UUID videoId) {
        try {
            logger.debug("Fetching latest prediction for video: {}", videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);
            if (!predictions.isEmpty()) {
                return ResponseEntity.ok(predictions.get(0)); // First is latest due to ORDER BY created_at DESC
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to fetch latest prediction for video: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAISystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean isReady = aiProcessingService.isAISystemReady();
            Map<String, Object> systemInfo = aiProcessingService.getSystemInfo();

            status.put("status", isReady ? "ready" : "not_ready");
            status.put("message", isReady ? "AI system is ready for processing" : "AI system is not available");
            status.put("timestamp", System.currentTimeMillis());
            status.put("systemInfo", systemInfo);

            logger.debug("AI system status check: {}", isReady ? "READY" : "NOT READY");
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error checking AI system status", e);
            status.put("status", "error");
            status.put("message", "Error checking AI system: " + e.getMessage());
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(status);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAIStatistics() {
        try {
            Map<String, Object> stats = aiProcessingService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get AI statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/{videoId}")
    public ResponseEntity<Map<String, Object>> getVideoStatistics(@PathVariable UUID videoId) {
        try {
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
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Failed to get video statistics for: " + videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/predictions/{videoId}")
    public ResponseEntity<Map<String, String>> deletePredictions(@PathVariable UUID videoId) {
        try {
            aiProcessingService.deletePredictionsByVideoId(videoId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "All predictions deleted for video: " + videoId);
            response.put("videoId", videoId.toString());

            logger.info("Deleted predictions for video: {}", videoId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete predictions for video: " + videoId, e);

            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete predictions: " + e.getMessage());
            response.put("videoId", videoId.toString());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
