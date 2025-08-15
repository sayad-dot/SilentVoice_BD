package com.example.silentvoice_bd.learning.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.silentvoice_bd.learning.dto.FeedbackRequest;
import com.example.silentvoice_bd.learning.dto.FeedbackResponse;
import com.example.silentvoice_bd.learning.model.FeedbackSession;
import com.example.silentvoice_bd.learning.repository.FeedbackSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LiveFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(LiveFeedbackService.class);

    @Autowired
    private FeedbackSessionRepository feedbackSessionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.ai.service.url:http://localhost:5000}")
    private String pythonAiUrl;

    // Active session tracking
    private final Map<String, LocalDateTime> activeSessions = new HashMap<>();

    public FeedbackResponse analyzePoseData(FeedbackRequest request, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("pose_landmarks", request.getPoseLandmarks());
            requestBody.put("lesson_id", request.getLessonId());
            requestBody.put("expected_sign", request.getExpectedSign());
            requestBody.put("user_id", userId.toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    pythonAiUrl + "/analyze_pose",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
            }
            );

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return createFallbackResponse("No response from AI service");
            }

            Double confidenceScore = getDoubleValue(result.get("confidence_score"));
            String feedbackText = (String) result.get("feedback_text");
            Boolean isCorrect = (Boolean) result.get("is_correct");
            String improvementTips = (String) result.get("improvement_tips");

            try {
                FeedbackSession session = new FeedbackSession();
                session.setUserId(userId);
                session.setLessonId(request.getLessonId());
                session.setPoseData(objectMapper.writeValueAsString(request.getPoseLandmarks()));
                session.setConfidenceScore(confidenceScore);
                session.setFeedbackText(feedbackText);
                session.setSessionId(request.getSessionId());
                session.setCreatedAt(LocalDateTime.now());

                feedbackSessionRepository.save(session);
            } catch (JsonProcessingException e) {
                logger.error("Error serializing pose data", e);
            }

            FeedbackResponse feedbackResponse = new FeedbackResponse();
            feedbackResponse.setConfidenceScore(confidenceScore);
            feedbackResponse.setFeedbackText(feedbackText);
            feedbackResponse.setIsCorrect(isCorrect != null ? isCorrect : confidenceScore > 70.0);
            feedbackResponse.setImprovementTips(improvementTips);
            feedbackResponse.setSessionId(request.getSessionId());
            feedbackResponse.setTimestamp(LocalDateTime.now());

            return feedbackResponse;

        } catch (RestClientException e) {
            logger.error("Error connecting to Python AI service", e);
            return createFallbackResponse("AI service temporarily unavailable. Please check your pose and try again.");
        } catch (Exception e) {
            logger.error("Unexpected error in pose analysis", e);
            return createFallbackResponse("Unable to analyze pose. Please ensure your hands are clearly visible.");
        }
    }

    public void startFeedbackSession(Long lessonId, UUID userId) {
        String sessionKey = userId + "_" + lessonId;
        activeSessions.put(sessionKey, LocalDateTime.now());

        FeedbackSession session = new FeedbackSession();
        session.setUserId(userId);
        session.setLessonId(lessonId);
        session.setSessionId(generateSessionId(userId, lessonId));
        session.setCreatedAt(LocalDateTime.now());
        session.setFeedbackText("Session started");
        session.setConfidenceScore(0.0);

        feedbackSessionRepository.save(session);
    }

    public void resetFeedbackSession(Long lessonId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            restTemplate.postForEntity(
                    pythonAiUrl + "/reset_session",
                    entity,
                    String.class
            );

            logger.info("Feedback session reset for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error resetting feedback session", e);
        }
    }

    public void endFeedbackSession(Long lessonId, UUID userId) {
        String sessionKey = userId + "_" + lessonId;
        LocalDateTime startTime = activeSessions.remove(sessionKey);

        if (startTime != null) {
            int sessionDuration = (int) java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();

            FeedbackSession session = new FeedbackSession();
            session.setUserId(userId);
            session.setLessonId(lessonId);
            session.setSessionId(generateSessionId(userId, lessonId));
            session.setSessionDuration(sessionDuration);
            session.setCreatedAt(LocalDateTime.now());
            session.setFeedbackText("Session ended");
            session.setConfidenceScore(0.0);

            feedbackSessionRepository.save(session);
        }
    }

    public Double getUserAverageConfidence(UUID userId, Long lessonId) {
        return feedbackSessionRepository.getAverageConfidenceByUserAndLesson(userId, lessonId);
    }

    public Long getSuccessfulSessionsCount(UUID userId) {
        return feedbackSessionRepository.countSuccessfulSessionsByUser(userId, 70.0);
    }

    private FeedbackResponse createFallbackResponse(String errorMessage) {
        FeedbackResponse fallbackResponse = new FeedbackResponse();
        fallbackResponse.setConfidenceScore(0.0);
        fallbackResponse.setFeedbackText(errorMessage);
        fallbackResponse.setIsCorrect(false);
        fallbackResponse.setTimestamp(LocalDateTime.now());
        fallbackResponse.setImprovementTips("Ensure good lighting and clear hand visibility");
        return fallbackResponse;
    }

    private String generateSessionId(UUID userId, Long lessonId) {
        return userId.toString().substring(0, 8) + "_" + lessonId + "_" + System.currentTimeMillis();
    }

    private Double getDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double d) {
            return d;
        }
        if (value instanceof Integer i) {
            return i.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
