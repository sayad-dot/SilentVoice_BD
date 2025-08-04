package com.example.silentvoice_bd.ai.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.ai.services.TextToSpeechService;
import com.example.silentvoice_bd.repository.SignLanguagePredictionRepository;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    private SignLanguagePredictionRepository predictionRepository;

    @GetMapping("/prediction/{predictionId}")
    public ResponseEntity<byte[]> getAudioForPrediction(@PathVariable UUID predictionId) {
        logger.info("🔊 Audio request for prediction: {}", predictionId);

        try {
            Optional<SignLanguagePrediction> predictionOpt = predictionRepository.findById(predictionId);
            if (predictionOpt.isEmpty()) {
                logger.warn("❌ Prediction not found: {}", predictionId);
                return ResponseEntity.notFound().build();
            }

            SignLanguagePrediction prediction = predictionOpt.get();
            logger.info("📝 Found prediction text: {}", prediction.getPredictedText());

            // Generate audio file path
            String audioFilePath = "./uploads/audio/prediction_" + predictionId + ".wav";

            // Check if audio file exists, if not create it
            byte[] audioData = textToSpeechService.getAudioFile(audioFilePath);
            if (audioData == null) {
                logger.info("🎵 Generating audio on-demand for: {}", prediction.getPredictedText());
                // Generate audio on-demand
                String generatedPath = textToSpeechService.convertTextToSpeech(
                        prediction.getPredictedText(),
                        prediction.getId()
                );
                if (generatedPath != null) {
                    audioData = textToSpeechService.getAudioFile(generatedPath);
                    logger.info("✅ Audio generated successfully");
                } else {
                    logger.error("❌ Failed to generate audio");
                }
            } else {
                logger.info("✅ Using existing audio file");
            }

            if (audioData != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("audio/wav"));
                headers.setContentLength(audioData.length);
                headers.set("Content-Disposition", "inline; filename=\"prediction_audio.wav\"");

                logger.info("🔊 Serving audio file, size: {} bytes", audioData.length);
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(audioData);
            }

            logger.error("❌ No audio data available for prediction: {}", predictionId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("💥 Error serving audio for prediction {}: {}", predictionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAudio(@RequestBody Map<String, String> request) {
        logger.info("🎵 Audio generation request received");

        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                logger.warn("⚠️ Empty text provided for audio generation");
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Text parameter is required")
                );
            }

            logger.info("📝 Generating audio for text: {}", text);
            UUID tempId = UUID.randomUUID();
            String audioFilePath = textToSpeechService.convertTextToSpeech(text, tempId);

            if (audioFilePath != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("audioUrl", "/api/audio/prediction/" + tempId);
                response.put("text", text);
                response.put("status", "success");
                response.put("predictionId", tempId);

                logger.info("✅ Audio generated successfully with ID: {}", tempId);
                return ResponseEntity.ok(response);
            } else {
                logger.error("❌ Failed to generate audio for text: {}", text);
                return ResponseEntity.internalServerError().body(
                        Map.of("error", "Failed to generate audio")
                );
            }

        } catch (Exception e) {
            logger.error("💥 Audio generation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal server error: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/prediction/{predictionId}")
    public ResponseEntity<Map<String, Object>> deleteAudio(@PathVariable UUID predictionId) {
        logger.info("🗑️ Audio deletion request for: {}", predictionId);

        try {
            boolean deleted = textToSpeechService.deleteAudioFile(predictionId);
            if (deleted) {
                logger.info("✅ Audio file deleted successfully: {}", predictionId);
                return ResponseEntity.ok(Map.of(
                        "message", "Audio file deleted successfully",
                        "predictionId", predictionId.toString()
                ));
            } else {
                logger.warn("⚠️ Audio file not found for deletion: {}", predictionId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("💥 Error deleting audio file {}: {}", predictionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to delete audio file")
            );
        }
    }
}
