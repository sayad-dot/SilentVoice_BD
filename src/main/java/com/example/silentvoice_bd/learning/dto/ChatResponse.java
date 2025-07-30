package com.example.silentvoice_bd.learning.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatResponse {

    private String content;
    private String sender;
    private LocalDateTime timestamp;
    private String contextData;
    private Boolean isError = false;
    private String errorMessage;
}
