package com.example.silentvoice_bd.learning.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {

    @JsonProperty("message")  // Maps frontend 'message' to backend 'content'
    private String content;

    private String sender; // USER, BOT

    @JsonProperty("lesson_context")
    private LessonContext lessonContext;

    private Long lessonId;
    private LocalDateTime timestamp;
    private String contextData; // JSON string for additional context

    // Inner class to handle lesson_context mapping
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LessonContext {

        @JsonProperty("lesson_id")
        private Long lessonId;

        @JsonProperty("lesson_title")
        private String lessonTitle;

        @JsonProperty("current_sign")
        private String currentSign;

        @JsonProperty("page_context")
        private String pageContext;

        @JsonProperty("page_features")
        private String[] pageFeatures;

        @JsonProperty("is_global")
        private Boolean isGlobal;
    }

    // Helper method to extract lessonId from context
    public Long getLessonId() {
        if (this.lessonId != null) {
            return this.lessonId;
        }
        if (this.lessonContext != null && this.lessonContext.getLessonId() != null) {
            return this.lessonContext.getLessonId();
        }
        return null;
    }

    // Helper method to get context data as JSON string
    public String getContextDataAsJson() {
        if (this.lessonContext != null) {
            try {
                return String.format("{\"lesson_title\":\"%s\",\"current_sign\":\"%s\",\"lesson_id\":%s,\"page_context\":\"%s\",\"is_global\":%s}",
                        lessonContext.getLessonTitle() != null ? lessonContext.getLessonTitle() : "",
                        lessonContext.getCurrentSign() != null ? lessonContext.getCurrentSign() : "",
                        lessonContext.getLessonId() != null ? lessonContext.getLessonId() : "null",
                        lessonContext.getPageContext() != null ? lessonContext.getPageContext() : "general",
                        lessonContext.getIsGlobal() != null ? lessonContext.getIsGlobal() : false);
            } catch (Exception e) {
                return "{}";
            }
        }
        return this.contextData != null ? this.contextData : "{}";
    }
}
