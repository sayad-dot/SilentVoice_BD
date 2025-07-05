package com.example.silentvoice_bd.ai.controllers;

import com.example.silentvoice_bd.ai.dto.AIProcessingRequest;
import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.ai.services.AIProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AIController {

    @Autowired
    private AIProcessingService aiProcessingService;

    @PostMapping("/predict/{videoId}")
    public CompletableFuture<ResponseEntity<PredictionResponse>> predictSignLanguageAsync(@PathVariable UUID videoId) {
        return aiProcessingService.processVideoAsync(videoId)
            .thenApply(response -> {
                if (response.isSuccess()) {
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .exceptionally(throwable -> {
                PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    @PostMapping("/predict/{videoId}/sync")
    public ResponseEntity<PredictionResponse> predictSignLanguageSync(@PathVariable UUID videoId) {
        try {
            PredictionResponse response = aiProcessingService.processVideoSync(videoId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            PredictionResponse errorResponse = PredictionResponse.error("Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<PredictionResponse>> processVideoRequest(@RequestBody AIProcessingRequest request) {
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
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/high-confidence")
    public ResponseEntity<List<SignLanguagePrediction>> getHighConfidencePredictions(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0.8") double minConfidence) {
        try {
            List<SignLanguagePrediction> predictions = aiProcessingService.getHighConfidencePredictions(videoId, minConfidence);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions/{videoId}/latest")
    public ResponseEntity<SignLanguagePrediction> getLatestPrediction(@PathVariable UUID videoId) {
        try {
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);
            if (!predictions.isEmpty()) {
                return ResponseEntity.ok(predictions.get(0)); // First is latest due to ORDER BY created_at DESC
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAISystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean isReady = aiProcessingService.isAISystemReady();
            status.put("status", isReady ? "ready" : "not_ready");
            status.put("message", isReady ? "AI system is ready for processing" : "AI system is not available");
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            status.put("status", "error");
            status.put("message", "Error checking AI system: " + e.getMessage());
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(status);
        }
    }

    @GetMapping("/statistics/{videoId}")
    public ResponseEntity<Map<String, Object>> getVideoStatistics(@PathVariable UUID videoId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            Long predictionCount = aiProcessingService.getPredictionCount(videoId);
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(videoId);

            stats.put("totalPredictions", predictionCount);
            stats.put("hasHighConfidencePredictions",
                predictions.stream().anyMatch(p -> p.getConfidenceScore().doubleValue() >= 0.8));

            if (!predictions.isEmpty()) {
                SignLanguagePrediction latest = predictions.get(0);
                stats.put("latestPrediction", latest.getPredictedText());
                stats.put("latestConfidence", latest.getConfidenceScore());
                stats.put("latestProcessingTime", latest.getProcessingTimeMs());
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
