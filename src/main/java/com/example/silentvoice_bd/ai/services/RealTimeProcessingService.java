package com.example.silentvoice_bd.ai.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealTimeProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeProcessingService.class);

    private final ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.python.venv.path:./ai-env/bin/python}")
    private String pythonVenv;

    @Value("${ai.python.scripts.path:./python-ai/scripts/}")
    private String scriptsPath;

    /**
     * Create a new session.
     */
    public Map<String, Object> createSession(String sessionId) {
        sessions.put(sessionId, new SessionData());
        logger.info("Created real-time session: {}", sessionId);
        return Map.of("success", true, "sessionId", sessionId);
    }

    /**
     * Process a base64-encoded frame.
     */
    public Map<String, Object> processFrame(String sessionId, String base64Frame) throws Exception {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            return Map.of("success", false, "error", "Session not found");
        }

        // Decode base64 to bytes
        byte[] bytes = Base64.getDecoder().decode(base64Frame);
        String encoded = Base64.getEncoder().encodeToString(bytes);

        // Call the Python realtime processor
        // realtime_processor.py frame <base64_string>
        JsonNode result = PythonCaller.callPython(
                pythonVenv,
                scriptsPath + "realtime_processor.py",
                "frame",
                encoded
        );

        // Update session counts
        data.framesProcessed++;
        if (result.path("prediction").path("success").asBoolean(false)) {
            data.predictionsMade++;
        }

        // Convert JsonNode to Map
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.convertValue(result, Map.class);
        map.put("success", result.path("success").asBoolean(true));
        return map;
    }

    /**
     * Get statistics for a session.
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            return Map.of("success", false, "error", "Session not found");
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("framesProcessed", data.framesProcessed);
        stats.put("predictionsMade", data.predictionsMade);
        stats.put("sessionId", sessionId);
        return Map.of("success", true, "stats", stats);
    }

    /**
     * Close (delete) a session.
     */
    public Map<String, Object> closeSession(String sessionId) {
        sessions.remove(sessionId);
        logger.info("Closed real-time session: {}", sessionId);
        return Map.of("success", true, "sessionId", sessionId);
    }

    /**
     * Internal holder for session data.
     */
    private static class SessionData {

        int framesProcessed = 0;
        int predictionsMade = 0;
    }
}
