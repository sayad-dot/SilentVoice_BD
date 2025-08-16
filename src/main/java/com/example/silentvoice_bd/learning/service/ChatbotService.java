package com.example.silentvoice_bd.learning.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.example.silentvoice_bd.learning.dto.ChatMessage;
import com.example.silentvoice_bd.learning.dto.ChatResponse;
import com.example.silentvoice_bd.learning.model.ChatConversation;
import com.example.silentvoice_bd.learning.repository.ChatConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private ChatConversationRepository conversationRepository;

    // Gemini Configuration
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String geminiApiUrl;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // OpenAI Configuration (backup)
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Main entry point for processing chat messages
     */
    public ChatResponse processMessage(ChatMessage message, UUID userId) {
        try {
            log.info("Processing message from user {}: {}", userId, message.getContent());

            // Validate input
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be null or empty");
            }

            // Save user message
            saveUserMessage(message, userId);

            // Get conversation history for context
            List<ChatConversation> history = getRecentHistory(userId, message.getLessonId());

            // Generate AI response (try Gemini first, then OpenAI as backup)
            String botReply = generateAIResponse(message.getContent(), history, message);

            // Save bot response
            saveBotMessage(botReply, message, userId);

            // Return response
            return createSuccessResponse(botReply);

        } catch (Exception e) {
            log.error("Error processing chatbot message for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("I'm having trouble responding right now. Please try again in a moment! 😊");
        }
    }

    /**
     * Generate AI response - tries Gemini first, then OpenAI, then fallback
     */
    /**
     * Generate AI response - tries Gemini first, then OpenAI, then fallback
     */
    private String generateAIResponse(String userMessage, List<ChatConversation> history, ChatMessage context) {
        log.info("=== AI RESPONSE GENERATION ===");

        // ✅ FIXED: Try Gemini first (correct validation)
        if (geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.equals("your-gemini-api-key-here")) {
            try {
                log.info("🤖 Using Google Gemini API for response");
                log.info("Gemini Key: {} chars, starts with: {}",
                        geminiApiKey.length(),
                        geminiApiKey.substring(0, Math.min(6, geminiApiKey.length())));
                return getGeminiResponse(userMessage, history, context);
            } catch (Exception e) {
                log.warn("🔄 Gemini API failed: {}, trying OpenAI backup", e.getMessage());
            }
        } else {
            log.warn("⚠️ Gemini API key not valid: null={}, empty={}, length={}",
                    geminiApiKey == null,
                    geminiApiKey != null ? geminiApiKey.isEmpty() : "N/A",
                    geminiApiKey != null ? geminiApiKey.length() : 0);
        }

        // Try OpenAI as backup
        if (openaiApiKey != null && !openaiApiKey.isEmpty() && !openaiApiKey.equals("your-openai-api-key-here")) {
            try {
                log.info("🔄 Using OpenAI API as backup");
                return getOpenAIResponse(userMessage, history, context);
            } catch (Exception e) {
                log.warn("⚠️ Both AI APIs failed, falling back to enhanced responses: {}", e.getMessage());
            }
        }

        // Fallback to enhanced rule-based responses
        log.info("📝 Using fallback rule-based responses");
        return getEnhancedFallbackResponse(userMessage, context);
    }

    private String getGeminiResponse(String userMessage, List<ChatConversation> history, ChatMessage context) {
        try {
            log.info("🌐 Making Gemini API request...");
            log.info("Model: {}", geminiModel);
            log.info("URL: {}", geminiApiUrl);

            // Build conversation context for Gemini
            String conversationContext = buildGeminiPrompt(userMessage, history, context);
            log.info("📝 Gemini prompt length: {} characters", conversationContext.length());

            // Create request payload for Gemini
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", conversationContext)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 250,
                            "topP", 0.8,
                            "topK", 10
                    )
            );

            // Set headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            log.info("📡 Sending request to Gemini...");
            String response = restTemplate.exchange(
                    geminiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            ).getBody();

            log.info("✅ Gemini response received: {} characters", response != null ? response.length() : 0);
            log.debug("Raw Gemini response: {}", response);

            // Parse response
            String parsedResponse = parseGeminiResponse(response);
            log.info("🎯 Parsed Gemini response: {}", parsedResponse.length() > 100
                    ? parsedResponse.substring(0, 100) + "..." : parsedResponse);

            return parsedResponse;

        } catch (RestClientResponseException e) {
            log.error("🚫 Gemini API HTTP error {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API call failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ Gemini API error: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt for Gemini API
     */
    private String buildGeminiPrompt(String userMessage, List<ChatConversation> history, ChatMessage context) {
        StringBuilder prompt = new StringBuilder();

        // System context
        String systemPrompt = buildSystemPrompt(context);
        prompt.append(systemPrompt).append("\n\n");

        // Add conversation history
        if (!history.isEmpty()) {
            prompt.append("Previous conversation:\n");
            Collections.reverse(history);
            for (ChatConversation conv : history.stream().limit(5).collect(Collectors.toList())) {
                String role = "USER".equals(conv.getSender()) ? "Human" : "Assistant";
                prompt.append(role).append(": ").append(conv.getMessage()).append("\n");
            }
            prompt.append("\n");
        }

        // Current user message
        String pageContext = extractPageContext(context);
        prompt.append("Current context: User is on ").append(pageContext).append(" page\n");
        prompt.append("Human: ").append(userMessage).append("\n");
        prompt.append("Assistant:");

        return prompt.toString();
    }

    /**
     * Parse Gemini API response
     */
    private String parseGeminiResponse(String response) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode candidates = jsonNode.get("candidates");

        if (candidates != null && candidates.isArray() && candidates.size() > 0) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode content = firstCandidate.get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        return text.asText().trim();
                    }
                }
            }
        }

        throw new Exception("Invalid Gemini response format: " + response);
    }

    /**
     * Get response from OpenAI GPT API (backup method)
     */
    private String getOpenAIResponse(String userMessage, List<ChatConversation> history, ChatMessage context) {
        try {
            log.info("🌐 Making OpenAI API request...");
            log.info("Model: {}", openaiModel);

            // Build messages for OpenAI
            List<Map<String, String>> messages = buildOpenAIMessages(userMessage, history, context);

            // Create request payload
            Map<String, Object> requestBody = Map.of(
                    "model", openaiModel,
                    "messages", messages,
                    "max_tokens", 250,
                    "temperature", 0.7
            );

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            log.info("📡 Sending request to OpenAI...");
            String response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            ).getBody();

            log.info("✅ OpenAI response received: {} characters", response != null ? response.length() : 0);

            // Parse response
            return parseOpenAIResponse(response);

        } catch (RestClientResponseException e) {
            log.error("🚫 OpenAI API HTTP error {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API call failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ OpenAI API error: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build messages array for OpenAI API
     */
    private List<Map<String, String>> buildOpenAIMessages(String userMessage, List<ChatConversation> history, ChatMessage context) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System message with context
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(context)));

        // Add conversation history (last 10 messages)
        Collections.reverse(history);
        for (ChatConversation conv : history.stream().limit(10).collect(Collectors.toList())) {
            String role = "USER".equals(conv.getSender()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", conv.getMessage()));
        }

        // Add current user message with context
        messages.add(Map.of("role", "user", "content", addContextToMessage(userMessage, context)));

        return messages;
    }

    /**
     * Build context-aware system prompt
     */
    private String buildSystemPrompt(ChatMessage context) {
        String basePrompt = "You are SignHelper, an intelligent and friendly AI assistant for SilentVoice BD, "
                + "a Bangla sign language learning platform. You are helpful, encouraging, and knowledgeable about "
                + "sign language, technology, and learning techniques. ";

        String pageContext = extractPageContext(context);

        switch (pageContext) {
            case "upload":
                return basePrompt + "The user is currently on the video upload page. Help them with file uploads, "
                        + "supported formats, AI processing, troubleshooting, and general sign language questions. "
                        + "Be technical yet encouraging and provide clear step-by-step guidance.";

            case "live":
                return basePrompt + "The user is using live recognition features. Assist with webcam setup, "
                        + "lighting optimization, hand positioning, real-time feedback, and practice tips. "
                        + "Be practical and supportive, focusing on improving their experience.";

            case "learning":
                return basePrompt + "The user is in the learning section. Provide lesson guidance, practice techniques, "
                        + "cultural context about Bangla sign language, motivation, and educational support. "
                        + "Be encouraging and educational, helping them progress in their learning journey.";

            default:
                return basePrompt + "The user may ask about any aspect of the application. Provide navigation help, "
                        + "feature explanations, sign language guidance, technical support, and motivational encouragement. "
                        + "Adapt your responses to their needs and maintain a friendly, helpful tone.";
        }
    }

    /**
     * Parse OpenAI API response
     */
    private String parseOpenAIResponse(String response) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode choices = jsonNode.get("choices");

        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText().trim();
                }
            }
        }

        throw new Exception("Invalid OpenAI response format");
    }

    // Keep ALL your existing fallback, database, and utility methods...
    /**
     * Enhanced fallback response system
     */
    private String getEnhancedFallbackResponse(String userMessage, ChatMessage context) {
        String pageContext = extractPageContext(context);
        String lowerMessage = userMessage.toLowerCase();

        // Greeting responses
        if (containsAny(lowerMessage, "hello", "hi", "hey", "start", "help")) {
            return getGreetingResponse(pageContext);
        }

        // Page-specific responses
        switch (pageContext) {
            case "upload":
                return getUploadPageResponse(lowerMessage);
            case "live":
                return getLivePageResponse(lowerMessage);
            case "learning":
                return getLearningPageResponse(lowerMessage);
            default:
                return getGeneralResponse(lowerMessage);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractPageContext(ChatMessage context) {
        try {
            String contextData = context.getContextDataAsJson();
            if (contextData.contains("\"page_context\":\"upload\"")) {
                return "upload";
            }
            if (contextData.contains("\"page_context\":\"live\"")) {
                return "live";
            }
            if (contextData.contains("\"page_context\":\"learning\"")) {
                return "learning";
            }
        } catch (Exception e) {
            log.debug("Could not extract page context: {}", e.getMessage());
        }
        return "general";
    }

    private String addContextToMessage(String userMessage, ChatMessage context) {
        String pageContext = extractPageContext(context);
        return String.format("[Context: user is on %s page] %s", pageContext, userMessage);
    }

    private String getUploadPageResponse(String message) {
        if (containsAny(message, "slow", "taking", "long", "time")) {
            return "Upload speed depends on your file size and internet connection. Large files (>50MB) may take longer. "
                    + "For best results, ensure stable internet and try uploading during off-peak hours. 📤";
        }
        if (containsAny(message, "format", "support", "type")) {
            return "We support MP4, AVI, MOV, and WMV formats. Maximum file size is 100MB. "
                    + "For optimal AI analysis, use good lighting and keep your hands clearly visible. 🎥";
        }
        if (containsAny(message, "analysis", "ai", "how", "work")) {
            return "Our AI extracts video frames, analyzes hand movements using deep learning, "
                    + "and matches patterns to Bangla signs. The process typically takes 30-90 seconds. 🤖";
        }
        if (containsAny(message, "error", "failed", "problem")) {
            return "Upload issues? Check: 1) File format (MP4/AVI/MOV), 2) Size <100MB, 3) Stable internet. "
                    + "Try refreshing or using a different browser if problems persist. 🔧";
        }
        return "I'm here to help with your video uploads! Ask about supported formats, file sizes, or troubleshooting. 📁";
    }

    private String getLivePageResponse(String message) {
        if (containsAny(message, "camera", "webcam", "not working", "black")) {
            return "Camera troubleshooting: 1) Allow browser permissions, 2) Close other apps using camera, "
                    + "3) Refresh page, 4) Try different browser. Check if camera works in other applications. 📹";
        }
        if (containsAny(message, "accuracy", "improve", "better")) {
            return "Improve recognition: 1) Good lighting (natural light is best), 2) Plain background, "
                    + "3) Keep hands centered, 4) Sign at normal speed, 5) Stay arm's length from camera. ✨";
        }
        if (containsAny(message, "lighting", "light")) {
            return "Optimal lighting tips: Face a window for natural light, avoid backlighting, "
                    + "ensure even lighting on both hands, avoid shadows. Good lighting dramatically improves accuracy! 💡";
        }
        return "I'm here to help with live recognition! Ask about camera setup, lighting, or accuracy tips. 🎯";
    }

    private String getLearningPageResponse(String message) {
        if (containsAny(message, "lesson", "start", "begin")) {
            return "Starting lessons: 1) Choose your level, 2) Watch demonstration videos, "
                    + "3) Practice with Mirror Mode, 4) Take your time to master each sign. Progress at your own pace! 📚";
        }
        if (containsAny(message, "practice", "improve", "technique")) {
            return "Practice tips: 1) Start slowly, 2) Focus on hand shapes, 3) Use mirror or webcam, "
                    + "4) Practice 15-20 min daily, 5) Record yourself for review. Consistency is key! 💪";
        }
        if (containsAny(message, "culture", "bangladesh", "deaf", "community")) {
            return "Bangla Sign Language serves 200,000+ deaf individuals in Bangladesh. It has rich cultural expressions "
                    + "and its own grammar. Learning it builds bridges and supports our deaf community! 🇧🇩";
        }
        if (containsAny(message, "difficult", "hard", "struggling")) {
            return "Learning can be challenging, but every expert was once a beginner! Break complex signs into steps, "
                    + "practice regularly, celebrate small wins, and be patient with yourself. You're doing great! 🌟";
        }
        return "I'm here to support your learning journey! Ask about lessons, practice tips, or Bangla sign culture. 🎓";
    }

    private String getGeneralResponse(String message) {
        if (containsAny(message, "navigation", "how to use", "guide")) {
            return "SilentVoice BD Navigation: 📤 Upload for video analysis, 📹 Live for real-time recognition, "
                    + "📚 Learn for structured lessons. I'm here to help with any feature! 🗺️";
        }
        if (containsAny(message, "motivation", "encourage")) {
            return "You're on an amazing journey learning sign language! Every step you take helps create "
                    + "a more inclusive world. Keep practicing, stay curious, and remember - progress over perfection! 🌈";
        }
        if (containsAny(message, "thank", "thanks", "good", "helpful")) {
            return "You're very welcome! I'm glad I could help. Keep exploring and practicing - "
                    + "I'm always here when you need assistance on your sign language journey! 😊";
        }
        return "I'm SignHelper, your AI assistant for SilentVoice BD! I can help with uploads, live recognition, "
                + "lessons, or any sign language questions. What would you like to know? 🤝";
    }

    private String getGreetingResponse(String pageContext) {
        switch (pageContext) {
            case "upload":
                return "👋 Hello! I'm here to help with video uploads and AI analysis. "
                        + "Upload your sign language videos and I'll guide you through the process!";
            case "live":
                return "🎥 Hi there! Ready for live sign recognition? I can help with camera setup, "
                        + "lighting tips, and improving your real-time accuracy!";
            case "learning":
                return "📚 Welcome to your learning journey! I'm here to help you master Bangla sign language "
                        + "with lessons, practice tips, and encouragement. Let's get started!";
            default:
                return "👋 Hello! I'm SignHelper, your AI companion for learning Bangla sign language. "
                        + "I'm here to help you navigate the app and improve your signing skills!";
        }
    }

    /**
     * Database operations
     */
    private ChatConversation saveUserMessage(ChatMessage message, UUID userId) {
        ChatConversation userConv = new ChatConversation();
        userConv.setUserId(userId);
        userConv.setLessonId(message.getLessonId());
        userConv.setMessage(message.getContent().trim());
        userConv.setSender("USER");
        userConv.setContextData(message.getContextDataAsJson());
        userConv.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(userConv);
    }

    private ChatConversation saveBotMessage(String content, ChatMessage originalMessage, UUID userId) {
        ChatConversation botConv = new ChatConversation();
        botConv.setUserId(userId);
        botConv.setLessonId(originalMessage.getLessonId());
        botConv.setMessage(content);
        botConv.setSender("BOT");
        botConv.setContextData(originalMessage.getContextDataAsJson());
        botConv.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(botConv);
    }

    private List<ChatConversation> getRecentHistory(UUID userId, Long lessonId) {
        return conversationRepository
                .findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, lessonId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Response creation helpers
     */
    private ChatResponse createSuccessResponse(String content) {
        ChatResponse response = new ChatResponse();
        response.setContent(content);
        response.setSender("BOT");
        response.setTimestamp(LocalDateTime.now());
        response.setIsError(false);
        return response;
    }

    private ChatResponse createErrorResponse(String message) {
        ChatResponse response = new ChatResponse();
        response.setContent(message);
        response.setSender("BOT");
        response.setTimestamp(LocalDateTime.now());
        response.setIsError(true);
        return response;
    }

    /**
     * Public utility methods
     */
    public List<ChatMessage> getConversationHistory(UUID userId, Long lessonId) {
        return conversationRepository
                .findByUserIdAndLessonIdOrderByCreatedAtAsc(userId, lessonId)
                .stream()
                .map(this::convertToMessage)
                .collect(Collectors.toList());
    }

    public void clearConversation(UUID userId, Long lessonId) {
        conversationRepository.deleteByUserIdAndLessonId(userId, lessonId);
        log.info("Cleared conversation for user {} and lesson {}", userId, lessonId);
    }

    public Long getUserMessageCount(UUID userId) {
        return conversationRepository.countUserMessagesByUser(userId);
    }

    private ChatMessage convertToMessage(ChatConversation conversation) {
        ChatMessage message = new ChatMessage();
        message.setContent(conversation.getMessage());
        message.setSender(conversation.getSender());
        message.setLessonId(conversation.getLessonId());
        message.setTimestamp(conversation.getCreatedAt());
        message.setContextData(conversation.getContextData());
        return message;
    }
}
