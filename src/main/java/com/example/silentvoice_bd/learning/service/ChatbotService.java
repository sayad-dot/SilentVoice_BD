package com.example.silentvoice_bd.learning.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.silentvoice_bd.learning.dto.ChatMessage;
import com.example.silentvoice_bd.learning.dto.ChatResponse;
import com.example.silentvoice_bd.learning.model.ChatConversation;
import com.example.silentvoice_bd.learning.repository.ChatConversationRepository;

import reactor.core.publisher.Mono;

@Service
public class ChatbotService {

    /* ──────────────────────────
       Dependencies & config
    ────────────────────────── */
    @Autowired
    private ChatConversationRepository conversationRepository;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private final WebClient webClient;

    public ChatbotService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /* ──────────────────────────
       Public entry point
    ────────────────────────── */
    public ChatResponse processMessage(ChatMessage message, UUID userId) {
        try {
            // Debug
            System.out.println("Processing msg: " + message.getContent());
            System.out.println("Context JSON: " + message.getContextDataAsJson());

            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be null or empty");
            }

            /* 1️⃣  Save user message */
            ChatConversation userConv = new ChatConversation();
            userConv.setUserId(userId);
            userConv.setLessonId(message.getLessonId());
            userConv.setMessage(message.getContent().trim());
            userConv.setSender("USER");
            userConv.setContextData(message.getContextDataAsJson());
            userConv.setCreatedAt(LocalDateTime.now());
            conversationRepository.save(userConv);

            /* 2️⃣  Fetch last 10 exchanges for context */
            List<ChatConversation> history = conversationRepository
                    .findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, message.getLessonId())
                    .stream().limit(10).collect(Collectors.toList());

            /* 3️⃣  Generate response */
            String botReply = generateBotResponse(message.getContent(), history, message);

            /* 4️⃣  Persist bot response */
            ChatConversation botConv = new ChatConversation();
            botConv.setUserId(userId);
            botConv.setLessonId(message.getLessonId());
            botConv.setMessage(botReply);
            botConv.setSender("BOT");
            botConv.setContextData(message.getContextDataAsJson());
            botConv.setCreatedAt(LocalDateTime.now());
            conversationRepository.save(botConv);

            /* 5️⃣  Return payload */
            ChatResponse resp = new ChatResponse();
            resp.setContent(botReply);
            resp.setSender("BOT");
            resp.setTimestamp(LocalDateTime.now());
            resp.setIsError(false);
            return resp;

        } catch (Exception e) {
            System.err.println("ChatbotService error: " + e.getMessage());
            ChatResponse err = new ChatResponse();
            err.setContent("I'm having trouble responding right now. Please try again in a moment.");
            err.setSender("BOT");
            err.setTimestamp(LocalDateTime.now());
            err.setIsError(true);
            err.setErrorMessage(e.getMessage());
            return err;
        }
    }

    /* ──────────────────────────
       Core response generator
    ────────────────────────── */
    private String generateBotResponse(String userMsg,
            List<ChatConversation> history,
            ChatMessage msgCtx) {

        /* If no OpenAI key → fallback engine */
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return getEnhancedFallbackResponse(userMsg, msgCtx);
        }

        try {
            /* 1. Build prompt list */
            List<Map<String, Object>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", buildSystemPrompt(msgCtx)));

            Collections.reverse(history); // chronological
            for (ChatConversation c : history) {
                msgs.add(Map.of("role", c.getSender().equals("USER") ? "user" : "assistant",
                        "content", c.getMessage()));
            }

            msgs.add(Map.of("role", "user",
                    "content", addContextToMessage(userMsg, msgCtx)));

            /* 2. OpenAI request */
            Map<String, Object> payload = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", msgs,
                    "max_tokens", 200,
                    "temperature", 0.7,
                    "presence_penalty", 0.1,
                    "frequency_penalty", 0.1
            );

            Mono<Map> responseMono = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> resp = responseMono.block();
            if (resp != null && resp.containsKey("choices")) {
                List<?> choices = (List<?>) resp.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
                    return msg.get("content").toString().trim();
                }
            }
            return getEnhancedFallbackResponse(userMsg, msgCtx);

        } catch (WebClientResponseException e) {
            System.err.println("OpenAI API error: " + e.getMessage());
            return getEnhancedFallbackResponse(userMsg, msgCtx);
        } catch (Exception e) {
            System.err.println("Unexpected OpenAI error: " + e.getMessage());
            return getEnhancedFallbackResponse(userMsg, msgCtx);
        }
    }

    /* ──────────────────────────
       Prompt & context helpers
    ────────────────────────── */
    private String buildSystemPrompt(ChatMessage ctx) {
        String base = "You are SignHelper, an intelligent AI assistant for SilentVoice BD (Bangla sign-language platform). ";

        switch (extractPageContext(ctx)) {
            case "upload":
                return base + "Currently on the video-upload screen. Help with upload steps, supported formats, file-size limits, AI processing times, and general sign-language questions. Be technical yet encouraging.";
            case "live":
                return base + "User is in live-recognition. Assist with webcam setup, lighting, positioning, real-time accuracy tips, and practice advice. Be practical and supportive.";
            case "learning":
                return base + "User is in structured lessons. Provide lesson navigation help, hand-position guidance, cultural context, and practice strategies. Be educational and motivating.";
            default:
                return base + "User may ask about any part of the app. Provide navigation help, sign-language guidance, technical support, and cultural insights. Adapt answers to user needs.";
        }
    }

    private String extractPageContext(ChatMessage ctx) {
        try {
            String json = ctx.getContextDataAsJson();
            if (json.contains("\"page_context\":\"upload\"")) {
                return "upload";
            }
            if (json.contains("\"page_context\":\"live\"")) {
                return "live";
            }
            if (json.contains("\"page_context\":\"learning\"")) {
                return "learning";
            }
        } catch (Exception ignored) {
        }
        return "general";
    }

    private String addContextToMessage(String userMsg, ChatMessage ctx) {
        return "[Context: user is on " + extractPageContext(ctx) + " page] " + userMsg;
    }

    /* ──────────────────────────
       Fallback engine (keyword rules)
    ────────────────────────── */
    private String getEnhancedFallbackResponse(String userMsg, ChatMessage ctx) {
        String pc = extractPageContext(ctx);

        // -------- Page-specific quick answers --------
        if (pc.equals("upload")) {
            if (has(userMsg, "why", "long", "taking", "slow", "time")) {
                return "Upload time depends on file size, internet speed and server load. Large files (>50MB) or slow networks take longer. Try a smaller file or check your connection.";
            }
            if (has(userMsg, "how", "analysis", "work", "ai", "recognize")) {
                return "Our AI grabs frames, analyses hand shapes/movements via deep-learning models, then matches patterns to Bangla signs. Whole process usually finishes in 30-90 seconds.";
            }
            if (has(userMsg, "format", "support", "type", "file")) {
                return "Supported video formats: MP4, AVI, MOV, WMV. Max file size: 100 MB. For best accuracy, ensure clear lighting and keep your hands fully visible.";
            }
            if (has(userMsg, "error", "failed", "not", "working", "problem")) {
                return "Upload issues? Check 1) format (MP4/AVI/MOV), 2) size <100 MB, 3) stable internet. Refresh or use another browser if it still fails.";
            }
        }

        if (pc.equals("live")) {
            if (has(userMsg, "camera", "webcam", "not", "working", "black")) {
                return "Camera tips: allow browser permissions, close other apps using camera, refresh the page or test with a different browser. Verify the camera works in another application.";
            }
            if (has(userMsg, "accuracy", "improve", "better", "recognition")) {
                return "Improve live accuracy: good lighting, plain background, keep hands centred, sign at normal speed and stay about an arm’s length from the camera.";
            }
            if (has(userMsg, "how", "start", "use", "live")) {
                return "Click ‘Start Camera’, grant permissions, ensure your hands are visible, then start signing. The AI will analyse each sign instantly.";
            }
        }

        if (pc.equals("learning")) {
            if (has(userMsg, "lesson", "start", "begin", "how")) {
                return "Open a lesson, watch the demonstration video, then practise. Use Mirror Practice to compare your signs in real time.";
            }
            if (has(userMsg, "practice", "improve", "better", "technique")) {
                return "Practice tips: start slowly, focus on accurate hand shapes, use a mirror or webcam, practise 15-20 min daily, and record yourself for review.";
            }
        }

        // -------- Cross-page intents --------
        if (has(userMsg, "how", "use", "app", "navigate", "around", "started", "help", "guide")) {
            return getAppGuidanceResponse(pc);
        }

        if (has(userMsg, "not", "working", "error", "problem", "issue", "trouble", "stuck", "broken")) {
            return getTechnicalSupportResponse(pc);
        }

        if (has(userMsg, "sign", "hand", "finger", "position", "movement", "gesture", "bangla")) {
            return "Keep hands fully visible, move deliberately, maintain consistent hand shapes and practise slowly at first. Consistency is crucial for accurate recognition.";
        }

        if (has(userMsg, "bangla", "bangladesh", "culture", "deaf", "community", "important", "why", "learn")) {
            return "Bangla Sign Language serves 200,000+ deaf individuals in Bangladesh. Learning it fosters inclusion, bridges communication gaps and supports the Deaf community’s rich culture.";
        }

        if (has(userMsg, "difficult", "hard", "frustrated", "can't", "struggling", "give up", "quit")) {
            return "Every expert was once a beginner! Practise steadily, celebrate small wins and be patient. Your dedication helps make communication inclusive for all. 💪";
        }

        if (has(userMsg, "thank", "thanks", "good", "great", "helpful", "awesome")) {
            return "You're welcome! I'm glad I could help. Keep exploring the app and practising your signs—I'm here whenever you need me.";
        }

        // -------- Default contextual fallback --------
        return getContextualDefaultResponse(pc);
    }

    /* ──────────────────────────
       Helper methods
    ────────────────────────── */
    private boolean has(String msg, String... keys) {
        String lower = msg.toLowerCase();
        for (String k : keys) {
            if (Pattern.compile("\\b" + Pattern.quote(k.toLowerCase()) + "\\b").matcher(lower).find()) {
                return true;
            }
        }
        return false;
    }

    private String getAppGuidanceResponse(String pc) {
        switch (pc) {
            case "upload":
                return "You're on the Video Upload page. Drag-and-drop or browse for a video (<100 MB). Enable AI analysis for automatic sign recognition once it finishes uploading.";
            case "live":
                return "This is Live Recognition. Click ‘Start Camera’, allow permissions and sign naturally while keeping hands visible. Real-time feedback will appear under the video.";
            case "learning":
                return "In Learn Signs you’ll find structured lessons with demo videos and Mirror Practice. Start with basics, then progress to words and phrases.";
            default:
                return "Use the top navigation: 📤 Upload for video analysis, 📹 Live for webcam recognition, 📚 Learn for structured lessons. Ask me anything along the way!";
        }
    }

    private String getTechnicalSupportResponse(String pc) {
        switch (pc) {
            case "upload":
                return "Upload troubles? Verify format (MP4/AVI/MOV), size <100 MB, stable internet, and try clearing cache or another browser.";
            case "live":
                return "Live Recognition issues? Check camera permissions, close other apps using camera, refresh page, try a different browser and ensure good lighting.";
            case "learning":
                return "Lesson page glitching? Refresh, check connection, clear cache or switch browsers. Let support know if it persists.";
            default:
                return "General tech tips: refresh page, check internet, clear cache, disable extensions or try a different browser.";
        }
    }

    private String getContextualDefaultResponse(String pc) {
        switch (pc) {
            case "upload":
                return "Need help with uploads or AI analysis? Ask me about supported formats, file sizes, processing time or sign-language queries!";
            case "live":
                return "I can help with live recognition setup, accuracy tips or signing questions. What would you like to know?";
            case "learning":
                return "Ask me anything about the lesson content, practice techniques or Bangla sign-language culture!";
            default:
                return "I'm SignHelper! I can guide you through uploads, live recognition, lessons or any sign-language questions. How can I assist?";
        }
    }

    /* ──────────────────────────
       Conversation utilities
    ────────────────────────── */
    public List<ChatMessage> getConversationHistory(UUID userId, Long lessonId) {
        return conversationRepository
                .findByUserIdAndLessonIdOrderByCreatedAtAsc(userId, lessonId)
                .stream()
                .map(conv -> {
                    ChatMessage m = new ChatMessage();
                    m.setContent(conv.getMessage());
                    m.setSender(conv.getSender());
                    m.setLessonId(conv.getLessonId());
                    m.setTimestamp(conv.getCreatedAt());
                    m.setContextData(conv.getContextData());
                    return m;
                }).collect(Collectors.toList());
    }

    public void clearConversation(UUID userId, Long lessonId) {
        conversationRepository.deleteByUserIdAndLessonId(userId, lessonId);
    }

    public Long getUserMessageCount(UUID userId) {
        return conversationRepository.countUserMessagesByUser(userId);
    }
}
