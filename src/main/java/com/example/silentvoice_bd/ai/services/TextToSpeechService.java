package com.example.silentvoice_bd.ai.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextToSpeechService {

    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);

    @Value("${tts.output.directory:./uploads/audio/}")
    private String audioOutputDirectory;

    @Value("${tts.python.script:./python-ai/scripts/text_to_speech.py}")
    private String ttsScriptPath;

    @Value("${tts.python.venv:./ai-env/bin/python}")
    private String pythonExecutable;

    public String convertTextToSpeech(String banglaText, UUID predictionId) {
        try {
            // Create audio output directory
            Path audioDir = Paths.get(audioOutputDirectory);
            Files.createDirectories(audioDir);

            // Generate unique audio filename
            String audioFilename = "prediction_" + predictionId + ".wav";
            String audioFilePath = audioDir.resolve(audioFilename).toString();

            // Execute Python TTS script
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    ttsScriptPath,
                    banglaText,
                    audioFilePath
            );

            processBuilder.directory(new File("."));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0 && Files.exists(Paths.get(audioFilePath))) {
                logger.info("Successfully generated speech for text: '{}' -> {}", banglaText, audioFilePath);
                return audioFilePath;
            } else {
                logger.error("TTS generation failed. Exit code: {}, Finished: {}",
                        process.exitValue(), finished);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating speech for text: " + banglaText, e);
            return null;
        }
    }

    public byte[] getAudioFile(String audioFilePath) {
        try {
            Path path = Paths.get(audioFilePath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            logger.error("Error reading audio file: " + audioFilePath, e);
        }
        return null;
    }

    public String getAudioUrl(UUID predictionId) {
        return "/api/audio/prediction/" + predictionId;
    }

    public boolean deleteAudioFile(UUID predictionId) {
        try {
            String audioFilename = "prediction_" + predictionId + ".wav";
            Path audioPath = Paths.get(audioOutputDirectory, audioFilename);

            if (Files.exists(audioPath)) {
                Files.delete(audioPath);
                logger.info("Deleted audio file: {}", audioPath);
                return true;
            }
        } catch (IOException e) {
            logger.error("Error deleting audio file for prediction: " + predictionId, e);
        }
        return false;
    }
}
