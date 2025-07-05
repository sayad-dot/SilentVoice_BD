package com.example.silentvoice_bd.ai.services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    
    @Autowired
    private PythonAIIntegrationService pythonIntegrationService;
    
    @Autowired
    private FrameExtractionService frameExtractionService;
    
    @Autowired
    private SignLanguagePredictionRepository predictionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Async("videoProcessingExecutor")
    public CompletableFuture<PredictionResponse> processVideoAsync(UUID videoFileId) {
        try {
            // Get extracted frames for the video
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);
            
            if (frames.isEmpty()) {
                PredictionResponse errorResponse = PredictionResponse.error("No frames found for video: " + videoFileId);
                return CompletableFuture.completedFuture(errorResponse);
            }
            
            // Process frames with AI
            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);
            
            // Save prediction to database if successful
            if (response.isSuccess()) {
                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
            }
            
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            PredictionResponse errorResponse = PredictionResponse.error("AI processing failed: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResponse);
        }
    }
    
    public PredictionResponse processVideoSync(UUID videoFileId) {
        try {
            List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoFileId);
            
            if (frames.isEmpty()) {
                return PredictionResponse.error("No frames found for video: " + videoFileId);
            }
            
            PredictionResponse response = pythonIntegrationService.processVideoFrames(frames);
            
            if (response.isSuccess()) {
                SignLanguagePrediction prediction = savePrediction(videoFileId, response, frames.size());
                response.setPredictionId(prediction.getId());
            }
            
            return response;
            
        } catch (Exception e) {
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
            
            prediction.setPredictionMetadata(objectMapper.writeValueAsString(metadata));
            
            return predictionRepository.save(prediction);
            
        } catch (Exception e) {
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
}
