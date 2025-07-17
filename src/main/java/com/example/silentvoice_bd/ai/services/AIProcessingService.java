package com.example.silentvoice_bd.ai.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.example.silentvoice_bd.processing.FrameExtractionService;
import com.example.silentvoice_bd.repository.SignLanguagePredictionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        logger.info("🚀 Starting async AI processing for video: {}", videoFileId);

        try {
            // Get extracted frames for the video
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);

            if (frames.isEmpty()) {
                logger.warn("❌ No frames found for video: {}", videoFileId);
                PredictionResponse errorResponse = PredictionResponse.error("No frames found for video: " + videoFileId);
                return CompletableFuture.completedFuture(errorResponse);
            }

            logger.info("📁 Found {} frames for video: {}", frames.size(), videoFileId);

            // CRITICAL: Log frame extraction details for debugging
            logger.info("🎞️ Frame extraction details for video {}:", videoFileId);
            logger.info("   📊 Total frames extracted: {}", frames.size());
            logger.info("   📂 Sample frame paths:");
            for (int i = 0; i < Math.min(3, frames.size()); i++) {
                logger.info("     Frame {}: {}", i + 1, frames.get(i).getFilePath());
            }

            // Process frames with AI
            logger.info("🤖 Starting Python AI integration for video: {}", videoFileId);
            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);

            // CRITICAL: Log AI processing results for debugging
            logger.info("🎯 AI Processing Results for video {}:", videoFileId);
            logger.info("   ✅ Success: {}", response.isSuccess());

            if (response.isSuccess()) {
                logger.info("   📝 Predicted text: '{}'", response.getPredictedText());
                logger.info("   📊 Confidence: {:.2f}%", response.getConfidence() * 100);
                logger.info("   ⏱️ Processing time: {} ms", response.getProcessingTimeMs());
                logger.info("   🏷️ Model version: {}", response.getModelVersion());

                // Check for normalization issues
                if (response.getConfidence() < 0.1) {
                    logger.error("❌ CRITICAL: Very low confidence ({:.2f}%) detected!", response.getConfidence() * 100);
                    logger.error("   🔧 This strongly suggests normalization issues in pose extraction");
                    logger.error("   📋 Raw prediction: {}", response.getPredictedText());

                    // Log processing info for debugging
                    if (response.getProcessingInfo() != null) {
                        logger.error("   📊 Processing info: {}", response.getProcessingInfo());
                    }
                } else if (response.getConfidence() > 0.7) {
                    logger.info("✅ High confidence prediction - normalization likely working correctly");
                } else {
                    logger.warn("⚠️ Medium confidence ({:.2f}%) - investigate normalization", response.getConfidence() * 100);
                }

                // Save prediction to database
                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
                response.setVideoId(videoFileId);

                logger.info("💾 Prediction saved to database with ID: {}", prediction.getId());
                logger.info("✅ AI processing completed successfully for video: {}", videoFileId);

            } else {
                logger.error("❌ AI processing failed for video: {}", videoFileId);
                logger.error("   🔍 Error details: {}", response.getError());

                // Log additional error context
                if (response.getProcessingInfo() != null) {
                    logger.error("   📊 Processing info: {}", response.getProcessingInfo());
                }
            }

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            logger.error("💥 AI processing failed for video: {} with exception", videoFileId, e);
            PredictionResponse errorResponse = PredictionResponse.error("AI processing failed: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResponse);
        }
    }

    public PredictionResponse processVideoSync(UUID videoFileId) {
        logger.info("🔄 Starting sync AI processing for video: {}", videoFileId);

        try {
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);

            if (frames.isEmpty()) {
                logger.warn("❌ No frames found for video: {}", videoFileId);
                return PredictionResponse.error("No frames found for video: " + videoFileId);
            }

            logger.info("📁 Found {} frames for sync processing of video: {}", frames.size(), videoFileId);

            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);

            if (response.isSuccess()) {
                logger.info("🎯 Sync AI processing results for video {}:", videoFileId);
                logger.info("   📝 Predicted text: '{}'", response.getPredictedText());
                logger.info("   📊 Confidence: {:.2f}%", response.getConfidence() * 100);

                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
                response.setVideoId(videoFileId);

                logger.info("✅ Sync AI processing completed for video: {}", videoFileId);
            } else {
                logger.error("❌ Sync AI processing failed for video: {}", videoFileId);
                logger.error("   🔍 Error: {}", response.getError());
            }

            return response;

        } catch (Exception e) {
            logger.error("💥 Sync AI processing failed for video: {} with exception", videoFileId, e);
            return PredictionResponse.error("AI processing failed: " + e.getMessage());
        }
    }

    private SignLanguagePrediction savePrediction(UUID videoFileId, PredictionResponse response, int frameCount) {
        try {
            logger.debug("💾 Saving prediction for video: {}", videoFileId);

            SignLanguagePrediction prediction = new SignLanguagePrediction();
            prediction.setVideoFileId(videoFileId);
            prediction.setPredictedText(response.getPredictedText());
            prediction.setConfidenceScore(BigDecimal.valueOf(response.getConfidence()));
            prediction.setProcessingTimeMs(response.getProcessingTimeMs());
            prediction.setModelVersion(response.getModelVersion());

            // Add comprehensive metadata for debugging
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("frameCount", frameCount);
            metadata.put("processingTimestamp", System.currentTimeMillis());
            metadata.put("confidence", response.getConfidence());
            metadata.put("confidencePercentage", response.getConfidence() * 100);

            // Flag for low confidence (normalization issue indicator)
            metadata.put("lowConfidenceFlag", response.getConfidence() < 0.1);
            metadata.put("highConfidenceFlag", response.getConfidence() > 0.7);

            if (response.getProcessingInfo() != null) {
                metadata.put("processingInfo", response.getProcessingInfo());
            }

            prediction.setPredictionMetadata(objectMapper.writeValueAsString(metadata));

            SignLanguagePrediction saved = predictionRepository.save(prediction);
            logger.debug("✅ Prediction saved to database with ID: {}", saved.getId());

            return saved;

        } catch (Exception e) {
            logger.error("💥 Failed to save prediction for video: {}", videoFileId, e);
            throw new RuntimeException("Failed to save prediction: " + e.getMessage(), e);
        }
    }

    public List<SignLanguagePrediction> getPredictionsByVideoId(UUID videoFileId) {
        logger.debug("🔍 Fetching predictions for video: {}", videoFileId);
        List<SignLanguagePrediction> predictions = predictionRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
        logger.debug("📊 Found {} predictions for video: {}", predictions.size(), videoFileId);
        return predictions;
    }

    public List<SignLanguagePrediction> getHighConfidencePredictions(UUID videoFileId, double minConfidence) {
        logger.debug("🔍 Fetching high confidence predictions (>{:.2f}%) for video: {}", minConfidence * 100, videoFileId);
        return predictionRepository.findHighConfidencePredictions(videoFileId, BigDecimal.valueOf(minConfidence));
    }

    public Long getPredictionCount(UUID videoFileId) {
        Long count = predictionRepository.countPredictionsByVideoId(videoFileId);
        logger.debug("📊 Prediction count for video {}: {}", videoFileId, count);
        return count;
    }

    public boolean isAISystemReady() {
        boolean ready = pythonIntegrationService.isAISystemReady();
        logger.info("🔧 AI system readiness check: {}", ready ? "READY" : "NOT READY");
        return ready;
    }

    public Map<String, Object> getSystemInfo() {
        logger.debug("ℹ️ Fetching AI system information");
        Map<String, Object> info = pythonIntegrationService.getSystemInfo();
        logger.debug("📊 System info retrieved: {} items", info.size());
        return info;
    }

    public Map<String, Object> getStatistics() {
        logger.info("📊 Generating AI processing statistics");
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

                // Log statistics for monitoring
                logger.info("📈 24h Statistics:");
                logger.info("   Average confidence: {:.2f}%", ((Double) accuracyStats[0]) * 100);
                logger.info("   Min confidence: {:.2f}%", ((Double) accuracyStats[1]) * 100);
                logger.info("   Max confidence: {:.2f}%", ((Double) accuracyStats[2]) * 100);
                logger.info("   Total predictions: {}", accuracyStats[3]);
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

                logger.info("📊 Model {} stats: {} predictions, {:.2f}% avg confidence",
                        modelVersion, count, avgConfidence * 100);
            }

            stats.put("modelStatistics", modelStatsMap);
            stats.put("systemReady", isAISystemReady());
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("💥 Failed to get AI statistics", e);
            stats.put("error", "Failed to get statistics: " + e.getMessage());
        }

        return stats;
    }

    public void deletePredictionsByVideoId(UUID videoFileId) {
        try {
            logger.info("🗑️ Deleting predictions for video: {}", videoFileId);
            predictionRepository.deleteByVideoFileId(videoFileId);
            logger.info("✅ Deleted all predictions for video: {}", videoFileId);
        } catch (Exception e) {
            logger.error("💥 Failed to delete predictions for video: {}", videoFileId, e);
            throw new RuntimeException("Failed to delete predictions: " + e.getMessage(), e);
        }
    }

    public Optional<SignLanguagePrediction> getLatestPrediction(UUID videoFileId) {
        try {
            logger.debug("🔍 Fetching latest prediction for video: {}", videoFileId);
            List<SignLanguagePrediction> predictions = predictionRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
            Optional<SignLanguagePrediction> latest = predictions.isEmpty() ? Optional.empty() : Optional.of(predictions.get(0));

            if (latest.isPresent()) {
                logger.debug("✅ Latest prediction found for video {}: '{}' with {:.2f}% confidence",
                        videoFileId, latest.get().getPredictedText(), latest.get().getConfidenceScore().doubleValue() * 100);
            } else {
                logger.debug("❌ No predictions found for video: {}", videoFileId);
            }

            return latest;
        } catch (Exception e) {
            logger.error("💥 Error getting latest prediction for video: {}", videoFileId, e);
            return Optional.empty();
        }
    }

    public boolean hasCompletedPrediction(UUID videoFileId) {
        boolean hasCompleted = getLatestPrediction(videoFileId).isPresent();
        logger.debug("🔍 Video {} has completed prediction: {}", videoFileId, hasCompleted);
        return hasCompleted;
    }
}
