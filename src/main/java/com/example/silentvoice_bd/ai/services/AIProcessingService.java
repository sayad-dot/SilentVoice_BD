package com.example.silentvoice_bd.ai.services;

import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.example.silentvoice_bd.processing.FrameExtractionService;
import com.example.silentvoice_bd.repository.SignLanguagePredictionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AIProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AIProcessingService.class);

    @Autowired
    private PythonAIIntegrationService pythonIntegrationService;

    @Autowired
    private FrameExtractionService frameExtractionService;

    @Autowired
    private SignLanguagePredictionRepository predictionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("videoProcessingExecutor")
    public CompletableFuture<PredictionResponse> processVideoAsync(UUID videoFileId) {
        logger.info("Starting async AI processing for video: {}", videoFileId);

        try {
            // Get extracted frames for the video
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);

            if (frames.isEmpty()) {
                logger.warn("No frames found for video: {}", videoFileId);
                PredictionResponse errorResponse = PredictionResponse.error("No frames found for video: " + videoFileId);
                return CompletableFuture.completedFuture(errorResponse);
            }

            logger.debug("Found {} frames for video: {}", frames.size(), videoFileId);

            // Process frames with AI
            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);

            // Save prediction to database if successful
            if (response.isSuccess()) {
                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
                response.setVideoId(videoFileId);

                logger.info("AI processing completed successfully for video: {}. Prediction ID: {}",
                           videoFileId, prediction.getId());
            } else {
                logger.error("AI processing failed for video: {}. Error: {}", videoFileId, response.getError());
            }

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            logger.error("AI processing failed for video: " + videoFileId, e);
            PredictionResponse errorResponse = PredictionResponse.error("AI processing failed: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResponse);
        }
    }

    public PredictionResponse processVideoSync(UUID videoFileId) {
        logger.info("Starting sync AI processing for video: {}", videoFileId);

        try {
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);

            if (frames.isEmpty()) {
                logger.warn("No frames found for video: {}", videoFileId);
                return PredictionResponse.error("No frames found for video: " + videoFileId);
            }

            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);

            if (response.isSuccess()) {
                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
                response.setVideoId(videoFileId);

                logger.info("Sync AI processing completed for video: {}. Prediction ID: {}",
                           videoFileId, prediction.getId());
            }

            return response;

        } catch (Exception e) {
            logger.error("Sync AI processing failed for video: " + videoFileId, e);
            return PredictionResponse.error("AI processing failed: " + e.getMessage());
        }
    }

    private SignLanguagePrediction savePrediction(UUID videoFileId, PredictionResponse response, int frameCount) {
        try {
            SignLanguagePrediction prediction = new SignLanguagePrediction();
            prediction.setVideoFileId(videoFileId);
            prediction.setPredictedText(response.getPredictedText());
            prediction.setConfidenceScore(BigDecimal.valueOf(response.getConfidence()));
            prediction.setProcessingTimeMs(response.getProcessingTimeMs());
            prediction.setModelVersion(response.getModelVersion());

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("frameCount", frameCount);
            metadata.put("processingTimestamp", System.currentTimeMillis());
            metadata.put("confidence", response.getConfidence());

            if (response.getProcessingInfo() != null) {
                metadata.put("processingInfo", response.getProcessingInfo());
            }

            prediction.setPredictionMetadata(objectMapper.writeValueAsString(metadata));

            SignLanguagePrediction saved = predictionRepository.save(prediction);
            logger.debug("Prediction saved to database with ID: {}", saved.getId());

            return saved;

        } catch (Exception e) {
            logger.error("Failed to save prediction for video: " + videoFileId, e);
            throw new RuntimeException("Failed to save prediction: " + e.getMessage(), e);
        }
    }

    public List<SignLanguagePrediction> getPredictionsByVideoId(UUID videoFileId) {
        return predictionRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
    }

    public List<SignLanguagePrediction> getHighConfidencePredictions(UUID videoFileId, double minConfidence) {
        return predictionRepository.findHighConfidencePredictions(videoFileId, BigDecimal.valueOf(minConfidence));
    }

    public Long getPredictionCount(UUID videoFileId) {
        return predictionRepository.countPredictionsByVideoId(videoFileId);
    }

    public boolean isAISystemReady() {
        return pythonIntegrationService.isAISystemReady();
    }

    public Map<String, Object> getSystemInfo() {
        return pythonIntegrationService.getSystemInfo();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get recent statistics
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Object[] accuracyStats = predictionRepository.getAccuracyStats(oneDayAgo);

            if (accuracyStats != null && accuracyStats.length >= 4) {
                stats.put("averageConfidence", accuracyStats[0]);
                stats.put("minConfidence", accuracyStats[1]);
                stats.put("maxConfidence", accuracyStats[2]);
                stats.put("totalPredictions24h", accuracyStats[3]);
            }

            // Get model version statistics
            List<Object[]> modelStats = predictionRepository.getStatsByModelVersion();
            Map<String, Map<String, Object>> modelStatsMap = new HashMap<>();

            for (Object[] stat : modelStats) {
                String modelVersion = (String) stat[0];
                Long count = (Long) stat[1];
                Double avgConfidence = (Double) stat[2];

                Map<String, Object> versionStats = new HashMap<>();
                versionStats.put("count", count);
                versionStats.put("averageConfidence", avgConfidence);

                modelStatsMap.put(modelVersion, versionStats);
            }

            stats.put("modelStatistics", modelStatsMap);
            stats.put("systemReady", isAISystemReady());
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Failed to get AI statistics", e);
            stats.put("error", "Failed to get statistics: " + e.getMessage());
        }

        return stats;
    }

    public void deletePredictionsByVideoId(UUID videoFileId) {
        try {
            predictionRepository.deleteByVideoFileId(videoFileId);
            logger.info("Deleted all predictions for video: {}", videoFileId);
        } catch (Exception e) {
            logger.error("Failed to delete predictions for video: " + videoFileId, e);
            throw new RuntimeException("Failed to delete predictions: " + e.getMessage(), e);
        }
    }
}
