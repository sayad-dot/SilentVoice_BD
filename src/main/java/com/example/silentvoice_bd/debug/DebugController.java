package com.example.silentvoice_bd.debug;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    // Gemini Configuration
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // OpenAI Configuration
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "Backend is running!");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    @GetMapping("/ai-config")
    public Map<String, Object> checkAiConfig() {
        Map<String, Object> config = new HashMap<>();

        // ✅ FIXED: Check Gemini properly
        boolean hasGeminiKey = geminiApiKey != null && !geminiApiKey.isEmpty()
                && !geminiApiKey.equals("your-gemini-api-key-here");

        // ✅ FIXED: Check OpenAI properly  
        boolean hasOpenaiKey = openaiApiKey != null && !openaiApiKey.isEmpty()
                && !openaiApiKey.equals("your-openai-api-key-here");

        config.put("gemini", Map.of(
                "hasApiKey", hasGeminiKey ? "YES" : "NO",
                "keyPrefix", hasGeminiKey && geminiApiKey.length() > 10
                ? geminiApiKey.substring(0, 6) + "..." : "EMPTY/INVALID",
                "keyLength", geminiApiKey != null ? geminiApiKey.length() : 0,
                "model", geminiModel,
                "fullKeyForDebug", geminiApiKey != null ? geminiApiKey : "NULL" // ⚠️ Remove this after testing
        ));

        config.put("openai", Map.of(
                "hasApiKey", hasOpenaiKey ? "YES" : "NO",
                "keyPrefix", hasOpenaiKey && openaiApiKey.length() > 10
                ? openaiApiKey.substring(0, 7) + "..." : "EMPTY/INVALID",
                "keyLength", openaiApiKey != null ? openaiApiKey.length() : 0,
                "model", openaiModel
        ));

        config.put("priority", "Gemini First, then OpenAI backup");
        config.put("timestamp", System.currentTimeMillis());

        return config;
    }
}
