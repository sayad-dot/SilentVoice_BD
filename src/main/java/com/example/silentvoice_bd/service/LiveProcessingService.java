package com.example.silentvoice_bd.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LiveProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(LiveProcessingService.class);

    @Value("${ai.python.venv.path:python-ai/ai-env-py311/bin/python}")
    private String pythonVenv;

    @Value("${ai.python.scripts.path:python-ai/scripts/}")
    private String scriptsPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, SessionFrameBuffer> sessionBuffers = new ConcurrentHashMap<>();

    // Inner class to manage frame sequences
    private static class SessionFrameBuffer {

        private final List<String> frames = new ArrayList<>();
        private final String sessionId;
        private final long createdAt;

        public SessionFrameBuffer(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
        }

        public synchronized boolean addFrame(String frameData) {
            if (frames.size() >= 30) {
                return false; // Buffer full
            }
            frames.add(frameData);
            logger.debug("📹 Added frame {}/30 to session {}", frames.size(), sessionId);
            return frames.size() == 30; // Return true when sequence complete
        }

        public synchronized List<String> getFramesAndClear() {
            List<String> result = new ArrayList<>(frames);
            frames.clear();
            return result;
        }

        public int getFrameCount() {
            return frames.size();
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    public String initializeLiveSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        sessionBuffers.put(sessionId, new SessionFrameBuffer(sessionId));
        logger.info("🎬 Initialized live session {} for user {}", sessionId, userId);
        return sessionId;
    }

    @Async
    public void processFrameAsync(String sessionId, String frameDataBase64,
            Long timestamp, Consumer<Map<String, Object>> callback) {
        try {
            SessionFrameBuffer buffer = sessionBuffers.get(sessionId);
            if (buffer == null) {
                logger.warn("⚠️ No buffer found for session: {}", sessionId);
                callback.accept(Map.of(
                        "error", true,
                        "message", "Session buffer not found",
                        "sessionId", sessionId
                ));
                return;
            }

            // Add frame to buffer
            boolean sequenceComplete = buffer.addFrame(frameDataBase64);
            logger.debug("📹 Frame {}/30 added to session {}", buffer.getFrameCount(), sessionId);

            if (sequenceComplete) {
                logger.info("✅ 30-frame sequence complete for session {}", sessionId);
                // Get frames and clear buffer
                List<String> frames = buffer.getFramesAndClear();
                // Process the complete sequence
                processFrameSequence(sessionId, frames, callback);
            } else {
                // Send progress update
                callback.accept(Map.of(
                        "progress", true,
                        "frame_count", buffer.getFrameCount(),
                        "message", "Collecting frames... " + buffer.getFrameCount() + "/30",
                        "sessionId", sessionId
                ));
            }

        } catch (Exception e) {
            logger.error("❌ Frame processing failed for session {}", sessionId, e);
            callback.accept(Map.of(
                    "error", true,
                    "message", "Frame processing failed: " + e.getMessage(),
                    "sessionId", sessionId,
                    "timestamp", timestamp
            ));
        }
    }

    private void processFrameSequence(String sessionId, List<String> frames,
            Consumer<Map<String, Object>> callback) {
        try {
            // Save frames temporarily with cleanup
            String tempSequencePath = saveFrameSequence(frames, sessionId);

            // Call Python script with timeout
            Map<String, Object> prediction = callPythonSequenceProcessor(tempSequencePath, sessionId);

            // Add session info
            prediction.put("sessionId", sessionId);
            prediction.put("timestamp", System.currentTimeMillis());

            logger.info("🔮 Generated prediction for session {}: {}",
                    sessionId, prediction.get("prediction"));

            // Send result
            callback.accept(prediction);

            // Cleanup temp files immediately
            cleanupTempSequence(tempSequencePath);

        } catch (Exception e) {
            logger.error("❌ Sequence processing failed for session {}", sessionId, e);
            callback.accept(Map.of(
                    "error", true,
                    "message", "Sequence processing failed: " + e.getMessage(),
                    "sessionId", sessionId
            ));
        }
    }

    private String saveFrameSequence(List<String> frames, String sessionId) throws IOException {
        String tempDir = "./temp/sequences/";
        Files.createDirectories(Paths.get(tempDir));
        String sequencePath = tempDir + sessionId + "_" + System.currentTimeMillis();

        // Create sequence directory
        Files.createDirectories(Paths.get(sequencePath));

        // Save each frame
        for (int i = 0; i < frames.size(); i++) {
            String frameData = frames.get(i);
            String base64Data = frameData.substring(frameData.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String framePath = sequencePath + "/frame_" + String.format("%03d", i) + ".jpg";
            Files.write(Paths.get(framePath), imageBytes);
        }

        logger.debug("💾 Saved 30-frame sequence to: {}", sequencePath);
        return sequencePath;
    }

    private Map<String, Object> callPythonSequenceProcessor(String sequencePath, String sessionId) throws Exception {
        CommandLine cmdLine = new CommandLine(pythonVenv);
        cmdLine.addArgument(scriptsPath + "enhanced_realtime_processor_fixed.py");  // Use enhanced processor
        cmdLine.addArgument("--sequence_path");
        cmdLine.addArgument(sequencePath);
        cmdLine.addArgument("--session_id");
        cmdLine.addArgument(sessionId);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);

        // Slightly longer timeout for enhanced processing
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000); // 30 seconds
        executor.setWatchdog(watchdog);

        try {
            long startTime = System.currentTimeMillis();
            logger.info("🤖 Starting enhanced processor for session: {}", sessionId);

            int exitValue = executor.execute(cmdLine);
            long duration = System.currentTimeMillis() - startTime;

            String output = outputStream.toString().trim();
            String errorOutput = errorStream.toString();

            logger.info("🤖 Enhanced processor completed in {}ms", duration);

            if (!errorOutput.isEmpty()) {
                logger.info("🐍 Enhanced processor log: {}", errorOutput);
            }

            if (exitValue == 0 && !output.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(output, Map.class);

                    // Log the actual prediction
                    String prediction = result.get("prediction").toString();
                    Object confidence = result.get("confidence");

                    logger.info("✅ Enhanced prediction successful for session: {} - '{}' ({})",
                            sessionId, prediction, confidence);

                    return result;

                } catch (Exception parseError) {
                    logger.error("❌ Failed to parse enhanced processor output: {}", parseError.getMessage());
                    throw new RuntimeException("Failed to parse enhanced processor output");
                }
            } else {
                logger.error("🤖 Enhanced processor failed (exit {}): {}", exitValue, errorOutput);
                throw new RuntimeException("Enhanced processor execution failed: " + errorOutput);
            }

        } catch (ExecuteException e) {
            if (watchdog.killedProcess()) {
                logger.error("⏰ Enhanced processor timed out for session: {}", sessionId);
                throw new RuntimeException("Enhanced processor execution timed out");
            } else {
                throw new RuntimeException("Enhanced processor execution failed with exit code: " + e.getExitValue());
            }
        }
    }

    private void cleanupTempSequence(String sequencePath) {
        try {
            // Delete all files in sequence directory
            Files.walk(Paths.get(sequencePath))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            logger.debug("🗑️ Cleaned up sequence: {}", sequencePath);
        } catch (IOException e) {
            logger.warn("⚠️ Failed to cleanup sequence: {}", sequencePath, e);
        }
    }

    public void cleanupSession(String sessionId) {
        try {
            SessionFrameBuffer buffer = sessionBuffers.remove(sessionId);
            if (buffer != null) {
                logger.info("🛑 Cleaned up session buffer for: {}", sessionId);
            }

            // Also cleanup any remaining temp files for this session
            cleanupSessionTempFiles(sessionId);
        } catch (Exception e) {
            logger.error("❌ Error cleaning up session {}", sessionId, e);
        }
    }

    private void cleanupSessionTempFiles(String sessionId) {
        try {
            String tempDir = "./temp/sequences/";
            if (Files.exists(Paths.get(tempDir))) {
                Files.list(Paths.get(tempDir))
                        .filter(path -> path.getFileName().toString().startsWith(sessionId + "_"))
                        .forEach(path -> {
                            try {
                                Files.walk(path)
                                        .sorted(Comparator.reverseOrder())
                                        .map(Path::toFile)
                                        .forEach(File::delete);
                                logger.debug("🗑️ Cleaned up session temp files: {}", path);
                            } catch (IOException e) {
                                logger.warn("⚠️ Failed to cleanup session temp files: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.warn("⚠️ Error cleaning up session temp files for {}", sessionId, e);
        }
    }

    /**
     * Get information about an active session
     */
    public SessionFrameBuffer getSessionInfo(String sessionId) {
        return sessionBuffers.get(sessionId);
    }

    /**
     * Get count of active sessions
     */
    public int getActiveSessionCount() {
        return sessionBuffers.size();
    }

    /**
     * Clean up inactive sessions (can be called periodically)
     */
    public void cleanupInactiveSessions(long inactiveThresholdMs) {
        long currentTime = System.currentTimeMillis();
        sessionBuffers.entrySet().removeIf(entry -> {
            SessionFrameBuffer buffer = entry.getValue();
            boolean isInactive = (currentTime - buffer.getCreatedAt()) > inactiveThresholdMs;
            if (isInactive) {
                logger.info("🧹 Removing inactive session {} (inactive for {} ms)",
                        entry.getKey(),
                        currentTime - buffer.getCreatedAt());
                // Cleanup temp files for inactive session
                cleanupSessionTempFiles(entry.getKey());
            }
            return isInactive;
        });
    }
}
