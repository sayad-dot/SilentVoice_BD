package com.example.silentvoice_bd.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.learning.dto.ChatMessage;
import com.example.silentvoice_bd.learning.dto.ChatResponse;
import com.example.silentvoice_bd.learning.service.ChatbotService;

@RestController
@RequestMapping("/api/learning/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatMessage message,
            @AuthenticationPrincipal User user) {
        ChatResponse response = chatbotService.processMessage(message, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversation/{lessonId}")
    public ResponseEntity<List<ChatMessage>> getConversation(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        List<ChatMessage> conversation = chatbotService.getConversationHistory(user.getId(), lessonId);
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/conversation/{lessonId}")
    public ResponseEntity<String> clearConversation(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal User user) {
        chatbotService.clearConversation(user.getId(), lessonId);
        return ResponseEntity.ok("Conversation cleared successfully");
    }
}
