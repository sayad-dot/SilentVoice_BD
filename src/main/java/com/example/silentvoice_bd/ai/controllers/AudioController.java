package com.example.silentvoice_bd.ai.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    private SignLanguagePredictionRepository predictionRepository;

    @GetMapping("/prediction/{predictionId}")
    public ResponseEntity<byte[]> getAudioForPrediction(@PathVariable UUID predictionId) {
        try {
            Optional<SignLanguagePrediction> predictionOpt = predictionRepository.findById(predictionId);

            if (predictionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SignLanguagePrediction prediction = predictionOpt.get();

            // Generate audio file path
            String audioFilePath = "./uploads/audio/prediction_" + predictionId + ".wav";

            // Check if audio file exists, if not create it
            byte[] audioData = textToSpeechService.getAudioFile(audioFilePath);

            if (audioData == null) {
                // Generate audio on-demand
                String generatedPath = textToSpeechService.convertTextToSpeech(
                        prediction.getPredictedText(),
                        prediction.getId()
                );

                if (generatedPath != null) {
                    audioData = textToSpeechService.getAudioFile(generatedPath);
                }
            }

            if (audioData != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("audio/wav"));
                headers.setContentLength(audioData.length);
                headers.set("Content-Disposition", "inline; filename=\"prediction_audio.wav\"");

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(audioData);
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAudio(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Text parameter is required")
                );
            }

            UUID tempId = UUID.randomUUID();
            String audioFilePath = textToSpeechService.convertTextToSpeech(text, tempId);

            if (audioFilePath != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("audioUrl", "/api/audio/prediction/" + tempId);
                response.put("text", text);
                response.put("status", "success");
                response.put("predictionId", tempId);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(
                        Map.of("error", "Failed to generate audio")
                );
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal server error: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/prediction/{predictionId}")
    public ResponseEntity<Map<String, String>> deleteAudio(@PathVariable UUID predictionId) {
        try {
            boolean deleted = textToSpeechService.deleteAudioFile(predictionId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "message", "Audio file deleted successfully",
                        "predictionId", predictionId.toString()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to delete audio file")
            );
        }
    }
}
