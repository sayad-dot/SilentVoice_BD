package com.example.silentvoice_bd.ai.services;

import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PythonAIIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(PythonAIIntegrationService.class);

    @Value("${ai.python.executable:python3}")
    private String pythonExecutable;

    @Value("${ai.python.scripts.path:./python-ai/scripts/}")
    private String pythonScriptsPath;

    @Value("${ai.python.timeout:120}")
    private int timeoutSeconds;

    @Value("${ai.python.venv.path:./ai-env/bin/python}")
    private String venvPythonPath;

    @Value("${ai.processing.frame-batch-size:20}")
    private int frameBatchSize;

    @Value("${ai.processing.max-frames-per-video:100}")
    private int maxFramesPerVideo;

    @Value("${ai.processing.retry-attempts:3}")
    private int retryAttempts;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PredictionResponse processVideoFrames(List<ExtractedFrame> frames) {
        logger.info("Processing {} frames for AI analysis", frames.size());

        try {
            long startTime = System.currentTimeMillis();

            // Step 1: Extract and limit frame paths
            List<String> framePaths = extractAndLimitFramePaths(frames);

            if (framePaths.isEmpty()) {
                logger.warn("No valid frame paths found for processing");
                return PredictionResponse.error("No valid frame paths found");
            }

            logger.info("Selected {} frames out of {} for processing", framePaths.size(), frames.size());

            // Step 2: Process frames in batches to avoid memory issues
            JsonNode poseResult = extractPosesInBatches(framePaths);

            if (!poseResult.get("success").asBoolean()) {
                String errorMsg = poseResult.has("error") ?
                    poseResult.get("error").asText() : "Unknown pose extraction error";
                logger.error("Pose extraction failed: {}", errorMsg);
                return PredictionResponse.error("Pose extraction failed: " + errorMsg);
            }

            logger.debug("Pose extraction successful, sequence length: {}",
                        poseResult.get("sequence_length").asInt());

            // Step 3: Predict sign language using LSTM
            JsonNode predictionResult = predictSignLanguageWithRetry(poseResult.get("pose_sequence"));

            if (!predictionResult.get("success").asBoolean()) {
                String errorMsg = predictionResult.has("error") ?
                    predictionResult.get("error").asText() : "Unknown prediction error";
                logger.error("Sign prediction failed: {}", errorMsg);
                return PredictionResponse.error("Prediction failed: " + errorMsg);
            }

            // Step 4: Create response
            int processingTime = (int) (System.currentTimeMillis() - startTime);

            PredictionResponse response = PredictionResponse.success(
                predictionResult.get("predicted_text").asText(),
                predictionResult.get("confidence").asDouble(),
                processingTime,
                null
            );

            response.setModelVersion(predictionResult.get("model_version").asText("bangla_lstm_v1"));

            // Add processing info
            Map<String, Object> processingInfo = new HashMap<>();
            processingInfo.put("totalFrames", frames.size());
            processingInfo.put("processedFrames", framePaths.size());
            processingInfo.put("batchSize", frameBatchSize);
            if (predictionResult.has("processing_info")) {
                processingInfo.putAll(objectMapper.convertValue(
                    predictionResult.get("processing_info"), Map.class));
            }
            response.setProcessingInfo(processingInfo);

            logger.info("AI processing completed successfully. Prediction: '{}', Confidence: {:.2f}%, Time: {}ms",
                       response.getPredictedText(), response.getConfidence() * 100, processingTime);

            return response;

        } catch (Exception e) {
            logger.error("AI processing failed with exception", e);
            return PredictionResponse.error("Processing failed: " + e.getMessage());
        }
    }

    private List<String> extractAndLimitFramePaths(List<ExtractedFrame> frames) {
        List<String> validPaths = new ArrayList<>();

        for (ExtractedFrame frame : frames) {
            if (frame.getFilePath() != null && new File(frame.getFilePath()).exists()) {
                validPaths.add(frame.getFilePath());
            } else {
                logger.debug("Frame file not found: {}", frame.getFilePath());
            }
        }

        // Limit number of frames and select evenly distributed samples
        if (validPaths.size() > maxFramesPerVideo) {
            List<String> selectedPaths = new ArrayList<>();
            int step = validPaths.size() / maxFramesPerVideo;

            for (int i = 0; i < validPaths.size(); i += step) {
                selectedPaths.add(validPaths.get(i));
                if (selectedPaths.size() >= maxFramesPerVideo) break;
            }

            logger.info("Reduced frames from {} to {} for processing efficiency",
                       validPaths.size(), selectedPaths.size());
            return selectedPaths;
        }

        return validPaths;
    }

    private JsonNode extractPosesInBatches(List<String> framePaths) throws Exception {
        List<List<Object>> allPoseSequences = new ArrayList<>();

        // Process frames in batches
        for (int i = 0; i < framePaths.size(); i += frameBatchSize) {
            int endIndex = Math.min(i + frameBatchSize, framePaths.size());
            List<String> batchPaths = framePaths.subList(i, endIndex);

            logger.debug("Processing batch {}/{}: {} frames",
                        (i / frameBatchSize) + 1,
                        (framePaths.size() + frameBatchSize - 1) / frameBatchSize,
                        batchPaths.size());

            String framePathsJson = objectMapper.writeValueAsString(batchPaths);
            JsonNode batchResult = executePythonScript("pose_extractor.py", "frames", framePathsJson);

            if (batchResult.get("success").asBoolean() && batchResult.has("pose_sequence")) {
                JsonNode poseSequence = batchResult.get("pose_sequence");
                for (JsonNode frame : poseSequence) {
                    allPoseSequences.add(objectMapper.convertValue(frame, List.class));
                }
            } else {
                logger.warn("Batch processing failed for frames {}-{}", i, endIndex - 1);
            }

            // Small delay between batches to prevent system overload
            if (i + frameBatchSize < framePaths.size()) {
                Thread.sleep(100);
            }
        }

        // Create combined result
        Map<String, Object> result = new HashMap<>();
        result.put("success", !allPoseSequences.isEmpty());
        result.put("pose_sequence", allPoseSequences);
        result.put("sequence_length", allPoseSequences.size());
        result.put("feature_dimension", allPoseSequences.isEmpty() ? 0 :
                  ((List<?>) allPoseSequences.get(0)).size());

        return objectMapper.valueToTree(result);
    }

    private JsonNode predictSignLanguageWithRetry(JsonNode poseSequence) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.debug("Sign prediction attempt {} of {}", attempt, retryAttempts);
                String poseSequenceJson = objectMapper.writeValueAsString(poseSequence);
                return executePythonScript("sign_predictor.py", poseSequenceJson);

            } catch (Exception e) {
                lastException = e;
                logger.warn("Prediction attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < retryAttempts) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }

        throw lastException;
    }

    private JsonNode executePythonScript(String scriptName, String... args) throws Exception {
        // Use virtual environment Python if available
        String pythonExec = new File(venvPythonPath).exists() ? venvPythonPath : pythonExecutable;

        // Build command
        CommandLine cmdLine = new CommandLine(pythonExec);
        cmdLine.addArgument(pythonScriptsPath + scriptName);

        for (String arg : args) {
            cmdLine.addArgument(arg, false);
        }

        logger.debug("Executing command: {}", cmdLine.toString());

        // Setup executor with increased timeout
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);

        // Extended timeout for heavy processing
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutSeconds * 1000L);
        executor.setWatchdog(watchdog);

        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);

        // Set environment variables to suppress TensorFlow warnings
        Map<String, String> environment = new HashMap<>();
        environment.put("TF_CPP_MIN_LOG_LEVEL", "2");
        environment.put("TF_ENABLE_ONEDNN_OPTS", "0");
        environment.put("PYTHONUNBUFFERED", "1");

        try {
            int exitCode = executor.execute(cmdLine, environment);

            String output = outputStream.toString().trim();
            String errorOutput = errorStream.toString().trim();

            if (exitCode != 0) {
                logger.error("Python script failed with exit code: {}", exitCode);
                if (!errorOutput.isEmpty()) {
                    logger.error("Python error output: {}", errorOutput);
                }
                throw new Exception("Python script failed with exit code: " + exitCode);
            }

            if (output.isEmpty()) {
                logger.error("Python script produced no output");
                if (!errorOutput.isEmpty()) {
                    logger.error("Python error output: {}", errorOutput);
                }
                throw new Exception("Python script produced no output");
            }

            logger.debug("Python script executed successfully, output length: {}", output.length());
            return objectMapper.readTree(output);

        } catch (ExecuteException e) {
            String errorOutput = errorStream.toString();

            if (e.getExitValue() == 143) {
                logger.error("Python script timed out after {} seconds", timeoutSeconds);
                throw new Exception("Python script timed out. Consider reducing frame count or increasing timeout.");
            }

            logger.error("Failed to execute Python script '{}': Exit code {}", scriptName, e.getExitValue());
            if (!errorOutput.isEmpty()) {
                logger.error("Python error output: {}", errorOutput);
            }
            throw new Exception("Failed to execute Python script '" + scriptName + "': " + e.getMessage());
        }
    }

    public boolean isAISystemReady() {
        try {
            // Quick readiness test with empty input
            JsonNode testResult = executePythonScript("sign_predictor.py", "[]");
            boolean isReady = testResult != null;

            logger.info("AI system readiness check: {}", isReady ? "READY" : "NOT READY");
            return isReady;

        } catch (Exception e) {
            logger.error("AI system readiness check failed", e);
            return false;
        }
    }

    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("pythonExecutable", pythonExecutable);
        info.put("venvPythonPath", venvPythonPath);
        info.put("scriptsPath", pythonScriptsPath);
        info.put("timeout", timeoutSeconds);
        info.put("frameBatchSize", frameBatchSize);
        info.put("maxFramesPerVideo", maxFramesPerVideo);
        info.put("retryAttempts", retryAttempts);
        info.put("venvExists", new File(venvPythonPath).exists());
        info.put("scriptsPathExists", new File(pythonScriptsPath).exists());
        return info;
    }
}
