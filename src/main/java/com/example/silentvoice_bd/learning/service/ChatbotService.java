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

    public ChatResponse processMessage(ChatMessage message, UUID userId) {
        try {
            // Debug logging
            System.out.println("Processing message: " + message.getContent());
            System.out.println("User ID: " + userId);
            System.out.println("Lesson ID: " + message.getLessonId());

            // Validate message content
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be null or empty");
            }

            // Save user message
            ChatConversation userMessage = new ChatConversation();
            userMessage.setUserId(userId);
            userMessage.setLessonId(message.getLessonId());
            userMessage.setMessage(message.getContent().trim());
            userMessage.setSender("USER");
            userMessage.setContextData(message.getContextDataAsJson());
            userMessage.setCreatedAt(LocalDateTime.now());

            conversationRepository.save(userMessage);

            // Get conversation context
            List<ChatConversation> recentConversation = conversationRepository
                    .findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, message.getLessonId())
                    .stream().limit(10).collect(Collectors.toList());

            // Generate bot response
            String botResponse = generateBotResponse(message.getContent(), recentConversation);

            // Save bot message
            ChatConversation botMessage = new ChatConversation();
            botMessage.setUserId(userId);
            botMessage.setLessonId(message.getLessonId());
            botMessage.setMessage(botResponse);
            botMessage.setSender("BOT");
            botMessage.setContextData(message.getContextDataAsJson());
            botMessage.setCreatedAt(LocalDateTime.now());

            conversationRepository.save(botMessage);

            ChatResponse response = new ChatResponse();
            response.setContent(botResponse);
            response.setSender("BOT");
            response.setTimestamp(LocalDateTime.now());
            response.setIsError(false);

            return response;

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setContent("I'm having trouble responding right now. Please try again in a moment.");
            errorResponse.setSender("BOT");
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setIsError(true);
            errorResponse.setErrorMessage(e.getMessage());
            return errorResponse;
        }
    }

    private String generateBotResponse(String userMessage, List<ChatConversation> context) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return getFallbackResponse(userMessage);
        }

        try {
            // Build conversation context for OpenAI
            List<Map<String, Object>> messages = new ArrayList<>();

            // System prompt - customize for sign language learning
            messages.add(Map.of("role", "system",
                    "content", "You are a helpful Bangla sign language learning assistant named 'SignHelper'. "
                    + "Your role is to help users learn Bangla sign language effectively. "
                    + "Guidelines:\n"
                    + "- Be encouraging and supportive\n"
                    + "- Provide specific, actionable feedback about hand positions and movements\n"
                    + "- Use simple, clear language\n"
                    + "- If users are struggling, break down signs into smaller steps\n"
                    + "- Encourage practice and patience\n"
                    + "- Share cultural context about Bangla sign language when relevant\n"
                    + "- Keep responses concise (2-3 sentences max)\n"
                    + "- Always stay positive and motivating"));

            // Add conversation history (reverse to get chronological order)
            Collections.reverse(context);
            for (ChatConversation conv : context) {
                if (conv.getSender().equals("USER")) {
                    messages.add(Map.of("role", "user", "content", conv.getMessage()));
                } else {
                    messages.add(Map.of("role", "assistant", "content", conv.getMessage()));
                }
            }

            // Add current message
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", messages,
                    "max_tokens", 150,
                    "temperature", 0.7,
                    "presence_penalty", 0.1,
                    "frequency_penalty", 0.1
            );

            Mono<Map> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> responseMap = response.block();
            if (responseMap != null && responseMap.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageContent = (Map<String, Object>) choice.get("message");
                    return messageContent.get("content").toString().trim();
                }
            }

            return getFallbackResponse(userMessage);

        } catch (WebClientResponseException e) {
            System.err.println("OpenAI API Error: " + e.getMessage());
            return getFallbackResponse(userMessage);
        } catch (Exception e) {
            System.err.println("Unexpected error in bot response generation: " + e.getMessage());
            return getFallbackResponse(userMessage);
        }
    }

    // Helper method to check if message contains any of the keywords as whole words
    private boolean containsKeywords(String message, String... keywords) {
        String lowerMessage = message.toLowerCase();
        for (String keyword : keywords) {
            // Use word boundary regex to match whole words only
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b");
            if (pattern.matcher(lowerMessage).find()) {
                return true;
            }
        }
        return false;
    }

    private String getFallbackResponse(String userMessage) {
        // Debug logging
        System.out.println("Using fallback response for: " + userMessage);

        // Use whole word matching to avoid false positives
        if (containsKeywords(userMessage, "hello", "hi", "hey", "start", "greetings")) {
            return "Hello! I'm here to help you learn Bangla sign language. What would you like to practice today?";

        } else if (containsKeywords(userMessage, "help", "confused", "don't understand", "stuck", "trouble", "issue", "problem", "struggling")) {
            return "I'm here to help! Can you tell me which specific sign you're having trouble with? I can guide you through the hand positions step by step.";

        } else if (containsKeywords(userMessage, "hand", "hands", "finger", "fingers", "position", "positioning", "placement")) {
            return "For better hand positioning, make sure your fingers are clearly visible and movements are distinct. Practice slowly first, then gradually increase speed!";

        } else if (containsKeywords(userMessage, "wrong", "mistake", "incorrect", "error", "bad", "not right")) {
            return "Don't worry about mistakes - they're part of learning! Try breaking down the sign into smaller movements and practice each part separately.";

        } else if (containsKeywords(userMessage, "good", "great", "correct", "right", "perfect", "excellent", "awesome")) {
            return "Excellent work! Keep practicing to build muscle memory. Consistency is key to mastering sign language.";

        } else if (containsKeywords(userMessage, "difficult", "hard", "challenging", "tough", "struggling", "can't")) {
            return "I understand it can be challenging! Remember, every expert was once a beginner. Take your time and practice regularly - you've got this!";

        } else if (containsKeywords(userMessage, "culture", "cultural", "bangladesh", "bengali", "bangla", "deaf", "community", "important", "significance")) {
            return "Bangla sign language is used by over 200,000 deaf individuals in Bangladesh. It has its own rich grammar and cultural expressions. Learning it helps build bridges in our community!";

        } else if (containsKeywords(userMessage, "practice", "practicing", "tip", "tips", "advice", "improve", "better", "how to")) {
            return "Try practicing in front of a mirror to see your signs clearly. Practice regularly for short periods rather than long sessions. Start with basic signs and gradually work up to more complex ones.";

        } else if (containsKeywords(userMessage, "mean", "meaning", "means", "what is", "what does", "definition", "translate")) {
            return "I can help explain sign meanings! Which specific sign would you like to know about? Describe the hand movement or gesture you're curious about.";

        } else if (containsKeywords(userMessage, "learn", "learning", "study", "studying", "teach", "education")) {
            return "Learning sign language is a wonderful journey! It takes time and practice, but every step helps you connect with the deaf community. What specific aspect would you like to focus on?";

        } else if (containsKeywords(userMessage, "thank", "thanks", "appreciate", "grateful")) {
            return "You're very welcome! I'm always here to help you on your sign language learning journey. Keep up the great work!";

        } else {
            return "I'm here to support your sign language learning! Feel free to ask about hand positions, sign meanings, practice tips, or anything else you'd like to know.";
        }
    }

    public List<ChatMessage> getConversationHistory(UUID userId, Long lessonId) {
        List<ChatConversation> conversations = conversationRepository
                .findByUserIdAndLessonIdOrderByCreatedAtAsc(userId, lessonId);

        return conversations.stream().map(conv -> {
            ChatMessage message = new ChatMessage();
            message.setContent(conv.getMessage());
            message.setSender(conv.getSender());
            message.setLessonId(conv.getLessonId());
            message.setTimestamp(conv.getCreatedAt());
            message.setContextData(conv.getContextData());
            return message;
        }).collect(Collectors.toList());
    }

    public void clearConversation(UUID userId, Long lessonId) {
        conversationRepository.deleteByUserIdAndLessonId(userId, lessonId);
    }

    public Long getUserMessageCount(UUID userId) {
        return conversationRepository.countUserMessagesByUser(userId);
    }
}
