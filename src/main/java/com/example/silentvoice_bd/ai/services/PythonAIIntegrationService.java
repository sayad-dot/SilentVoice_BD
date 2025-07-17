package com.example.silentvoice_bd.ai.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        logger.info("🎬 Processing {} frames for AI analysis", frames.size());

        try {
            long startTime = System.currentTimeMillis();

            // Step 1: Enhanced frame selection with motion-based prioritization
            List<String> framePaths = extractAndLimitFramePathsEnhanced(frames);

            if (framePaths.isEmpty()) {
                logger.warn("❌ No valid frame paths found for processing");
                return PredictionResponse.error("No valid frame paths found");
            }

            logger.info("📁 Selected {} frames out of {} for processing", framePaths.size(), frames.size());

            // CRITICAL: Log frame processing details for debugging
            logger.info("🎞️ Frame processing details:");
            logger.info("   📊 Total frames available: {}", frames.size());
            logger.info("   📂 Frames selected for processing: {}", framePaths.size());
            logger.info("   🔧 Batch size: {}", frameBatchSize);
            logger.info("   ⏱️ Max frames per video: {}", maxFramesPerVideo);

            // Step 2: Process frames with enhanced quality validation
            logger.info("🔄 Starting pose extraction with quality validation...");
            JsonNode poseResult = extractPosesWithQualityValidation(framePaths);

            if (!poseResult.get("success").asBoolean()) {
                String errorMsg = poseResult.has("error")
                        ? poseResult.get("error").asText() : "Unknown pose extraction error";
                logger.error("❌ Pose extraction failed: {}", errorMsg);
                return PredictionResponse.error("Pose extraction failed: " + errorMsg);
            }

            // CRITICAL: Log pose extraction results for debugging
            logger.info("✅ Pose extraction successful:");
            logger.info("   📊 Sequence length: {}", poseResult.get("sequence_length").asInt());
            logger.info("   🔢 Feature dimension: {}", poseResult.get("feature_dimension").asInt());
            logger.info("   🎯 Normalized flag: {}", poseResult.has("normalized") ? poseResult.get("normalized").asBoolean() : "unknown");
            logger.info("   📈 Data quality score: {}", poseResult.has("quality_score") ? poseResult.get("quality_score").asDouble() : "unknown");

            // Step 3: Predict sign language using LSTM
            logger.info("🤖 Starting sign language prediction...");
            JsonNode predictionResult = predictSignLanguageWithRetry(poseResult.get("pose_sequence"));

            if (!predictionResult.get("success").asBoolean()) {
                String errorMsg = predictionResult.has("error")
                        ? predictionResult.get("error").asText() : "Unknown prediction error";
                logger.error("❌ Sign prediction failed: {}", errorMsg);
                return PredictionResponse.error("Prediction failed: " + errorMsg);
            }

            // Step 4: Create response with comprehensive logging
            int processingTime = (int) (System.currentTimeMillis() - startTime);

            PredictionResponse response = PredictionResponse.success(
                    predictionResult.get("predicted_text").asText(),
                    predictionResult.get("confidence").asDouble(),
                    processingTime,
                    null
            );

            response.setModelVersion(predictionResult.get("model_version").asText("bangla_lstm_v1"));

            // CRITICAL: Log final prediction results for debugging
            logger.info("🎯 Final AI Processing Results:");
            logger.info("   📝 Predicted text: '{}'", response.getPredictedText());
            logger.info("   📊 Confidence: {:.2f}%", response.getConfidence() * 100);
            logger.info("   ⏱️ Processing time: {} ms", processingTime);
            logger.info("   🏷️ Model version: {}", response.getModelVersion());

            // Add comprehensive processing info for debugging
            Map<String, Object> processingInfo = new HashMap<>();
            processingInfo.put("totalFrames", frames.size());
            processingInfo.put("processedFrames", framePaths.size());
            processingInfo.put("batchSize", frameBatchSize);
            processingInfo.put("processingTimeMs", processingTime);
            processingInfo.put("poseExtractionSuccess", true);
            processingInfo.put("sequenceLength", poseResult.get("sequence_length").asInt());
            processingInfo.put("featureDimension", poseResult.get("feature_dimension").asInt());
            processingInfo.put("qualityScore", poseResult.has("quality_score") ? poseResult.get("quality_score").asDouble() : 0.0);
            processingInfo.put("frameSelectionMethod", "motion-based");

            if (predictionResult.has("processing_info")) {
                processingInfo.putAll(objectMapper.convertValue(
                        predictionResult.get("processing_info"), Map.class));
            }

            response.setProcessingInfo(processingInfo);

            // CRITICAL: Enhanced confidence analysis
            double confidence = response.getConfidence();
            if (confidence < 0.1) {
                logger.error("❌ CRITICAL: Very low confidence ({:.2f}%) detected!", confidence * 100);
                logger.error("   🔧 Possible causes: normalization issues, poor pose detection, or model mismatch");
                logger.error("   📊 Processing info: {}", processingInfo);
            } else if (confidence < 0.3) {
                logger.warn("⚠️ Low confidence ({:.2f}%) - investigate data quality", confidence * 100);
            } else if (confidence < 0.7) {
                logger.info("📊 Medium confidence ({:.2f}%) - acceptable but could be improved", confidence * 100);
            } else {
                logger.info("✅ High confidence ({:.2f}%) - excellent prediction quality", confidence * 100);
            }

            logger.info("✅ AI processing completed successfully. Prediction: '{}', Confidence: {:.2f}%, Time: {}ms",
                    response.getPredictedText(), response.getConfidence() * 100, processingTime);

            return response;

        } catch (Exception e) {
            logger.error("💥 AI processing failed with exception", e);
            return PredictionResponse.error("Processing failed: " + e.getMessage());
        }
    }

    // ENHANCED: Motion-based frame selection for better discrimination
    private List<String> extractAndLimitFramePathsEnhanced(List<ExtractedFrame> frames) {
        logger.debug("📁 Extracting and validating frame paths with motion-based selection...");

        List<ExtractedFrame> validFrames = new ArrayList<>();
        int invalidCount = 0;

        // Step 1: Filter valid frames
        for (ExtractedFrame frame : frames) {
            if (frame.getFilePath() != null && new File(frame.getFilePath()).exists()) {
                validFrames.add(frame);
            } else {
                logger.debug("❌ Frame file not found: {}", frame.getFilePath());
                invalidCount++;
            }
        }

        logger.info("📊 Frame validation results: {} valid, {} invalid out of {} total frames",
                validFrames.size(), invalidCount, frames.size());

        if (validFrames.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 2: Enhanced frame selection strategy
        List<String> selectedPaths = new ArrayList<>();

        if (validFrames.size() <= maxFramesPerVideo) {
            // Use all frames if within limit
            selectedPaths = validFrames.stream()
                    .map(ExtractedFrame::getFilePath)
                    .collect(Collectors.toList());
            logger.info("✅ Using all {} frames (within limit)", validFrames.size());
        } else {
            // Enhanced selection: prioritize frames with motion scores
            logger.info("🔄 Applying enhanced frame selection (motion-based)");

            // Sort by motion score (if available), then by keyframe status, then by timestamp
            validFrames.sort((f1, f2) -> {
                // Primary: Motion score (higher is better)
                if (f1.getMotionScore() != null && f2.getMotionScore() != null) {
                    // FIXED: Use BigDecimal's compareTo method instead of Double.compare
                    int motionCompare = f2.getMotionScore().compareTo(f1.getMotionScore());
                    if (motionCompare != 0) {
                        return motionCompare;
                    }
                }

                // Secondary: Keyframe status (keyframes are preferred)
                if (f1.getIsKeyframe() != null && f2.getIsKeyframe() != null) {
                    int keyframeCompare = Boolean.compare(f2.getIsKeyframe(), f1.getIsKeyframe());
                    if (keyframeCompare != 0) {
                        return keyframeCompare;
                    }
                }

                // Tertiary: Timestamp (chronological order)
                return f1.getTimestampSeconds().compareTo(f2.getTimestampSeconds());
            });

            // Select top frames based on scoring
            int selectedCount = Math.min(maxFramesPerVideo, validFrames.size());

            // Ensure we get a good temporal distribution
            if (selectedCount > 10) {
                // Use a hybrid approach: 70% best motion scores, 30% temporal distribution
                int motionBasedCount = (int) (selectedCount * 0.7);
                int temporalCount = selectedCount - motionBasedCount;

                // Add motion-based frames
                for (int i = 0; i < motionBasedCount && i < validFrames.size(); i++) {
                    selectedPaths.add(validFrames.get(i).getFilePath());
                }

                // Add temporally distributed frames
                int step = validFrames.size() / temporalCount;
                for (int i = 0; i < validFrames.size() && selectedPaths.size() < selectedCount; i += step) {
                    String path = validFrames.get(i).getFilePath();
                    if (!selectedPaths.contains(path)) {
                        selectedPaths.add(path);
                    }
                }
            } else {
                // For small numbers, just use the top-scored frames
                for (int i = 0; i < selectedCount; i++) {
                    selectedPaths.add(validFrames.get(i).getFilePath());
                }
            }

            logger.info("✅ Enhanced selection: {} frames chosen from {} candidates", selectedPaths.size(), validFrames.size());
            logger.info("   🎯 Selection criteria: motion score + keyframe status + temporal distribution");
        }

        return selectedPaths;
    }

    // ENHANCED: Pose extraction with quality validation
    private JsonNode extractPosesWithQualityValidation(List<String> framePaths) throws Exception {
        logger.info("🔄 Starting pose extraction with quality validation...");
        List<List<Object>> allPoseSequences = new ArrayList<>();
        List<Double> qualityScores = new ArrayList<>();
        int validSequences = 0;
        int totalSequences = 0;

        int totalBatches = (framePaths.size() + frameBatchSize - 1) / frameBatchSize;
        logger.info("📊 Processing {} frames in {} batches of size {}", framePaths.size(), totalBatches, frameBatchSize);

        // Process frames in batches
        for (int i = 0; i < framePaths.size(); i += frameBatchSize) {
            int endIndex = Math.min(i + frameBatchSize, framePaths.size());
            List<String> batchPaths = framePaths.subList(i, endIndex);
            int batchNumber = (i / frameBatchSize) + 1;

            logger.info("🔄 Processing batch {}/{}: {} frames", batchNumber, totalBatches, batchPaths.size());

            String framePathsJson = objectMapper.writeValueAsString(batchPaths);

            // CRITICAL: Log Python script execution for debugging
            logger.debug("🐍 Executing pose_extractor.py for batch {}", batchNumber);
            JsonNode batchResult = executePythonScript("pose_extractor.py", "frames", framePathsJson);

            if (batchResult.get("success").asBoolean() && batchResult.has("pose_sequence")) {
                JsonNode poseSequence = batchResult.get("pose_sequence");
                int sequenceLength = poseSequence.size();

                logger.debug("✅ Batch {} successful: {} pose sequences extracted", batchNumber, sequenceLength);

                // Enhanced quality validation
                for (JsonNode frame : poseSequence) {
                    List<Object> frameData = objectMapper.convertValue(frame, List.class);
                    totalSequences++;

                    // Validate frame data quality
                    double qualityScore = calculatePoseQualityScore(frameData);
                    qualityScores.add(qualityScore);

                    if (isValidPoseData(frameData, qualityScore)) {
                        allPoseSequences.add(frameData);
                        validSequences++;
                    } else {
                        logger.debug("⚠️ Skipping low-quality pose data in batch {} (quality: {:.2f})", batchNumber, qualityScore);
                    }
                }
            } else {
                logger.warn("❌ Batch {} processing failed for frames {}-{}", batchNumber, i, endIndex - 1);
                if (batchResult.has("error")) {
                    logger.warn("   Error details: {}", batchResult.get("error").asText());
                }
            }

            // Small delay between batches to prevent system overload
            if (i + frameBatchSize < framePaths.size()) {
                Thread.sleep(100);
            }
        }

        // Calculate overall quality metrics
        double averageQuality = qualityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double validityPercentage = totalSequences > 0 ? (validSequences * 100.0 / totalSequences) : 0.0;

        logger.info("📊 Data quality analysis:");
        logger.info("   ✅ Valid sequences: {}/{} ({:.1f}%)", validSequences, totalSequences, validityPercentage);
        logger.info("   📈 Average quality score: {:.2f}", averageQuality);
        logger.info("   🎯 Quality threshold: 0.3 (minimum acceptable)");

        // Create combined result with quality metrics
        Map<String, Object> result = new HashMap<>();
        result.put("success", !allPoseSequences.isEmpty());
        result.put("pose_sequence", allPoseSequences);
        result.put("sequence_length", allPoseSequences.size());
        result.put("feature_dimension", allPoseSequences.isEmpty() ? 0 : ((List<?>) allPoseSequences.get(0)).size());
        result.put("quality_score", averageQuality);
        result.put("validity_percentage", validityPercentage);
        result.put("total_processed", totalSequences);
        result.put("valid_sequences", validSequences);

        logger.info("✅ Pose extraction completed: {} total sequences, {} features per frame, {:.2f} avg quality",
                allPoseSequences.size(),
                allPoseSequences.isEmpty() ? 0 : ((List<?>) allPoseSequences.get(0)).size(),
                averageQuality);

        return objectMapper.valueToTree(result);
    }

    // ENHANCED: Calculate pose quality score
    private double calculatePoseQualityScore(List<Object> frameData) {
        if (frameData == null || frameData.size() != 288) {
            return 0.0;
        }

        double[] values = frameData.stream()
                .mapToDouble(obj -> ((Number) obj).doubleValue())
                .toArray();

        // Calculate quality metrics
        long zeroCount = Arrays.stream(values).filter(v -> Math.abs(v) < 1e-6).count();
        double zeroPercentage = zeroCount / (double) values.length;

        // Calculate variance (higher variance = more motion/detail)
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);

        // Calculate quality score (0.0 to 1.0)
        double qualityScore = (1.0 - zeroPercentage) * 0.7 + Math.min(variance * 1000, 1.0) * 0.3;

        return Math.max(0.0, Math.min(1.0, qualityScore));
    }

    // ENHANCED: Validate pose data quality
    private boolean isValidPoseData(List<Object> frameData, double qualityScore) {
        if (frameData == null || frameData.size() != 288) {
            return false;
        }

        // Quality score threshold (can be adjusted)
        return qualityScore >= 0.3;
    }

    private JsonNode predictSignLanguageWithRetry(JsonNode poseSequence) throws Exception {
        logger.info("🤖 Starting sign language prediction with retry mechanism...");
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.info("🔄 Sign prediction attempt {} of {}", attempt, retryAttempts);
                String poseSequenceJson = objectMapper.writeValueAsString(poseSequence);

                // CRITICAL: Log prediction input for debugging
                logger.debug("📊 Prediction input: {} sequences",
                        poseSequence.isArray() ? poseSequence.size() : "unknown");

                JsonNode result = executePythonScript("sign_predictor.py", poseSequenceJson);

                logger.info("✅ Sign prediction attempt {} successful", attempt);
                return result;

            } catch (Exception e) {
                lastException = e;
                logger.warn("❌ Prediction attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < retryAttempts) {
                    int sleepTime = 1000 * attempt;
                    logger.info("⏳ Waiting {} ms before retry attempt {}", sleepTime, attempt + 1);
                    Thread.sleep(sleepTime); // Exponential backoff
                }
            }
        }

        logger.error("💥 All {} prediction attempts failed", retryAttempts);
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

        // CRITICAL: Log Python execution for debugging
        logger.debug("🐍 Executing Python script: {}", scriptName);
        logger.debug("   Command: {}", cmdLine.toString());
        logger.debug("   Python executable: {}", pythonExec);
        logger.debug("   Scripts path: {}", pythonScriptsPath);
        logger.debug("   Arguments count: {}", args.length);

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
            long scriptStartTime = System.currentTimeMillis();
            int exitCode = executor.execute(cmdLine, environment);
            long scriptExecutionTime = System.currentTimeMillis() - scriptStartTime;

            String output = outputStream.toString().trim();
            String errorOutput = errorStream.toString().trim();

            // CRITICAL: Log Python script execution results
            logger.debug("✅ Python script '{}' completed in {} ms", scriptName, scriptExecutionTime);
            logger.debug("   Exit code: {}", exitCode);
            logger.debug("   Output length: {} characters", output.length());

            if (!errorOutput.isEmpty()) {
                logger.debug("   Error output: {}", errorOutput);
            }

            if (exitCode != 0) {
                logger.error("❌ Python script '{}' failed with exit code: {}", scriptName, exitCode);
                if (!errorOutput.isEmpty()) {
                    logger.error("   Python error output: {}", errorOutput);
                }
                throw new Exception("Python script failed with exit code: " + exitCode);
            }

            if (output.isEmpty()) {
                logger.error("❌ Python script '{}' produced no output", scriptName);
                if (!errorOutput.isEmpty()) {
                    logger.error("   Python error output: {}", errorOutput);
                }
                throw new Exception("Python script produced no output");
            }

            logger.debug("✅ Python script '{}' executed successfully", scriptName);
            JsonNode result = objectMapper.readTree(output);

            // CRITICAL: Log Python script results for debugging
            if (result.has("success")) {
                logger.debug("   Script success: {}", result.get("success").asBoolean());
            }
            if (result.has("confidence")) {
                logger.debug("   Confidence: {:.2f}%", result.get("confidence").asDouble() * 100);
            }
            if (result.has("predicted_text")) {
                logger.debug("   Predicted text: '{}'", result.get("predicted_text").asText());
            }

            return result;

        } catch (ExecuteException e) {
            String errorOutput = errorStream.toString();

            if (e.getExitValue() == 143) {
                logger.error("❌ Python script '{}' timed out after {} seconds", scriptName, timeoutSeconds);
                throw new Exception("Python script timed out. Consider reducing frame count or increasing timeout.");
            }

            logger.error("❌ Failed to execute Python script '{}': Exit code {}", scriptName, e.getExitValue());
            if (!errorOutput.isEmpty()) {
                logger.error("   Python error output: {}", errorOutput);
            }
            throw new Exception("Failed to execute Python script '" + scriptName + "': " + e.getMessage());
        }
    }

    public boolean isAISystemReady() {
        logger.info("🔧 Checking AI system readiness...");

        try {
            // Quick readiness test with empty input
            JsonNode testResult = executePythonScript("sign_predictor.py", "[]");
            boolean isReady = testResult != null && testResult.has("success");

            logger.info("🔧 AI system readiness check: {}", isReady ? "READY" : "NOT READY");

            if (isReady) {
                logger.info("✅ AI system is ready for processing");
            } else {
                logger.warn("❌ AI system is not ready - check Python environment and model files");
            }

            return isReady;

        } catch (Exception e) {
            logger.error("❌ AI system readiness check failed", e);
            return false;
        }
    }

    public Map<String, Object> getSystemInfo() {
        logger.debug("ℹ️ Gathering system information...");

        Map<String, Object> info = new HashMap<>();
        info.put("pythonExecutable", pythonExecutable);
        info.put("venvPythonPath", venvPythonPath);
        info.put("scriptsPath", pythonScriptsPath);
        info.put("timeout", timeoutSeconds);
        info.put("frameBatchSize", frameBatchSize);
        info.put("maxFramesPerVideo", maxFramesPerVideo);
        info.put("retryAttempts", retryAttempts);

        // Check file system status
        boolean venvExists = new File(venvPythonPath).exists();
        boolean scriptsPathExists = new File(pythonScriptsPath).exists();

        info.put("venvExists", venvExists);
        info.put("scriptsPathExists", scriptsPathExists);

        // Log system status
        logger.info("📊 System Information:");
        logger.info("   Python executable: {}", pythonExecutable);
        logger.info("   Virtual env path: {} (exists: {})", venvPythonPath, venvExists);
        logger.info("   Scripts path: {} (exists: {})", pythonScriptsPath, scriptsPathExists);
        logger.info("   Timeout: {} seconds", timeoutSeconds);
        logger.info("   Frame batch size: {}", frameBatchSize);
        logger.info("   Max frames per video: {}", maxFramesPerVideo);

        return info;
    }
}
