// src/main/java/com/example/silentvoice_bd/config/ChatbotConfig.java
package com.example.silentvoice_bd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatbotConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
